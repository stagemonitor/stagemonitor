package org.stagemonitor.collector.ehcache;

import com.codahale.metrics.MetricRegistry;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.stagemonitor.collector.core.Configuration;
import org.stagemonitor.collector.core.StageMonitorPlugin;
import org.stagemonitor.collector.core.rest.RestClient;

import static com.codahale.metrics.MetricRegistry.name;
import static org.stagemonitor.collector.core.util.GraphiteEncoder.encodeForGraphite;

public class EhCachePlugin implements StageMonitorPlugin {

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
		final CacheManager cacheManager = CacheManager.getCacheManager(configuration.getEhCacheName());
		for (String cacheName : cacheManager.getCacheNames()) {
			final Cache cache = cacheManager.getCache(cacheName);
			cache.setStatisticsEnabled(true);

			final String metricPrefix = name("cache", encodeForGraphite(cache.getName()));
			final StagemonitorCacheUsageListener cacheUsageListener = new StagemonitorCacheUsageListener(metricPrefix, metricRegistry);
			cache.registerCacheUsageListener(cacheUsageListener);
			metricRegistry.registerAll(new EhCacheMetricSet(metricPrefix, cache, cacheUsageListener));
		}

		RestClient.sendGrafanaDashboardAsync(configuration.getElasticsearchUrl(), "EhCache.json");
	}
}
