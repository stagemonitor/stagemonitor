package org.stagemonitor.ehcache;

import com.codahale.metrics.MetricRegistry;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.ConfigurationOption;
import org.stagemonitor.core.StageMonitorPlugin;
import org.stagemonitor.core.rest.RestClient;

import static com.codahale.metrics.MetricRegistry.name;
import static org.stagemonitor.core.util.GraphiteSanitizer.sanitizeGraphiteMetricSegment;

public class EhCachePlugin implements StageMonitorPlugin {

	public static final String EHCACHE_NAME = "stagemonitor.ehcache.name";

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
		addConfigOptions(configuration);
		final CacheManager cacheManager = CacheManager.getCacheManager(configuration.getString(EHCACHE_NAME));
		for (String cacheName : cacheManager.getCacheNames()) {
			final Cache cache = cacheManager.getCache(cacheName);
			cache.setStatisticsEnabled(true);

			final String metricPrefix = name("cache", sanitizeGraphiteMetricSegment(cache.getName()));
			final StagemonitorCacheUsageListener cacheUsageListener = new StagemonitorCacheUsageListener(metricPrefix, metricRegistry);
			cache.registerCacheUsageListener(cacheUsageListener);
			metricRegistry.registerAll(new EhCacheMetricSet(metricPrefix, cache, cacheUsageListener));
		}

		RestClient.sendGrafanaDashboardAsync(configuration.getElasticsearchUrl(), "EhCache.json");
	}

	private void addConfigOptions(Configuration config) {
		config.add(ConfigurationOption.builder()
				.key(EHCACHE_NAME)
				.dynamic(false)
				.label("EhCache cache name")
				.description("The name of the ehcache to instrument (the value of the 'name' attribute of the " +
						"'ehcache' tag in ehcache.xml)")
				.defaultValue(null)
				.build());
	}
}
