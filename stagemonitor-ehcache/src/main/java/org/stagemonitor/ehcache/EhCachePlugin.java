package org.stagemonitor.ehcache;

import java.util.Collections;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
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
			.defaultValue(true)
			.configurationCategory("EhCache Plugin")
			.build();

	@Override
	public void initializePlugin(Metric2Registry metricRegistry, Configuration configuration) {
		final EhCachePlugin chCacheConfig = configuration.getConfig(EhCachePlugin.class);
		final CacheManager cacheManager = CacheManager.getCacheManager(chCacheConfig.ehCacheNameOption.getValue());
		for (String cacheName : cacheManager.getCacheNames()) {
			final Cache cache = cacheManager.getCache(cacheName);
			cache.setStatisticsEnabled(true);

			final StagemonitorCacheUsageListener cacheUsageListener = new StagemonitorCacheUsageListener(cache.getName(),
					metricRegistry, chCacheConfig.timeGet.getValue());
			cache.registerCacheUsageListener(cacheUsageListener);
			metricRegistry.registerAll(new EhCacheMetricSet(cache.getName(), cache, cacheUsageListener));
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

	@Override
	public List<String> getPathsOfWidgetMetricTabPlugins() {
		return Collections.singletonList("/stagemonitor/static/tabs/metrics/ehcache-metrics");
	}
}
