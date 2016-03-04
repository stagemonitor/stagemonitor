package org.stagemonitor.ehcache;

import java.util.Collections;
import java.util.List;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.grafana.GrafanaClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

public class EhCachePlugin extends StagemonitorPlugin {

	private final ConfigurationOption<String> ehCacheNameOption = ConfigurationOption.stringOption()
			.key("stagemonitor.ehcache.name")
			.dynamic(false)
			.label("EhCache cache name")
			.description("The name of the ehcache to instrument (the value of the `name` attribute of the " +
					"`ehcache` tag in ehcache.xml)")
			.defaultValue(null)
			.configurationCategory("EhCache Plugin")
			.build();
	private final ConfigurationOption<Boolean> timeGet = ConfigurationOption.booleanOption()
			.key("stagemonitor.ehcache.get.timer")
			.dynamic(true)
			.label("Create timer for cache gets")
			.description("If set to true, a timer for each cache will be created which measures the time to get a " +
					"element from the cache. If you have a lot of caches, that could lead to a increased network and " +
					"disk utilisation. If set to false, only a meter (which measures the rate) will be created")
			.defaultValue(false)
			.configurationCategory("EhCache Plugin")
			.build();

	private Metric2Registry metricRegistry;

	@Override
	public void initializePlugin(Metric2Registry metricRegistry, Configuration configuration) {
		this.metricRegistry = metricRegistry;
		final EhCachePlugin ehCacheConfig = configuration.getConfig(EhCachePlugin.class);
		if (ehCacheConfig.ehCacheNameOption.getValue() != null) {
			final CacheManager cacheManager = CacheManager.getCacheManager(ehCacheConfig.ehCacheNameOption.getValue());
			monitorCaches(cacheManager);
		}

		final CorePlugin corePlugin = configuration.getConfig(CorePlugin.class);
		ElasticsearchClient elasticsearchClient = corePlugin.getElasticsearchClient();
		final GrafanaClient grafanaClient = corePlugin.getGrafanaClient();
		if (corePlugin.isReportToGraphite()) {
			elasticsearchClient.sendGrafana1DashboardAsync("grafana/Grafana1GraphiteEhCache.json");
		}
		if (corePlugin.isReportToElasticsearch()) {
			grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchEhCache.json");
			elasticsearchClient.sendBulkAsync("kibana/EhCache.bulk");
		}
	}

	/**
	 * You can use this method to manually register a {@link CacheManager} that should be monitored by stagemonitor.
	 * <p>
	 * Example:
	 * <pre>
	 * Stagemonitor.getConfiguration(EhCachePlugin.class).monitorCaches(yourCacheManager);
	 * </pre>
	 *
	 * @param cacheManager The CacheManager to monitor
	 */
	public void monitorCaches(CacheManager cacheManager) {
		for (String cacheName : cacheManager.getCacheNames()) {
			monitorCache(cacheManager.getCache(cacheName));
		}
	}

	/**
	 * You can use this method to manually register a {@link Cache} that should be monitored by stagemonitor.
	 * <p>
	 * Example:
	 * <pre>
	 * Stagemonitor.getConfiguration(EhCachePlugin.class).monitorCache(yourCache);
	 * </pre>
	 *
	 * @param cache The Cache to monitor
	 */
	public void monitorCache(Ehcache cache) {
		cache.setStatisticsEnabled(true);
		final StagemonitorCacheUsageListener cacheUsageListener = new StagemonitorCacheUsageListener(cache.getName(),
				metricRegistry, timeGet.getValue());
		cache.registerCacheUsageListener(cacheUsageListener);
		metricRegistry.registerAny(new EhCacheMetricSet(cache.getName(), cache, cacheUsageListener));
	}

	@Override
	public List<String> getPathsOfWidgetMetricTabPlugins() {
		return Collections.singletonList("/stagemonitor/static/tabs/metrics/ehcache-metrics");
	}
}
