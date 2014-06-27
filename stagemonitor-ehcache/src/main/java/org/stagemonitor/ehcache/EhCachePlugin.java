package org.stagemonitor.ehcache;

import com.codahale.metrics.MetricRegistry;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.StageMonitorPlugin;
import org.stagemonitor.core.rest.RestClient;

import static com.codahale.metrics.MetricRegistry.name;
import static org.stagemonitor.core.util.GraphiteSanitizer.sanitizeGraphiteMetricSegment;

public class EhCachePlugin implements StageMonitorPlugin {

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
		final CacheManager cacheManager = CacheManager.getCacheManager(configuration.getEhCacheName());
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
}
