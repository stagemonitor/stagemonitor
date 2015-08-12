package org.stagemonitor.ehcache;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.HashMap;
import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.RatioGauge;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.metrics.metrics2.Metric2Set;

/**
 * An instrumented {@link net.sf.ehcache.Ehcache} instance.
 */
public class EhCacheMetricSet implements Metric2Set {

	private final String cacheName;
	private final Ehcache cache;
	private final StagemonitorCacheUsageListener cacheUsageListener;

	public EhCacheMetricSet(String cacheName, Cache cache, StagemonitorCacheUsageListener cacheUsageListener) {
		this.cacheName = cacheName;
		this.cache = cache;
		this.cacheUsageListener = cacheUsageListener;
	}

	@Override
	public Map<MetricName, Metric> getMetrics() {
		final Map<MetricName, Metric> metrics = new HashMap<MetricName, Metric>();

		metrics.put(name("total_hit_ratio").tag("cache_name", cacheName).build(), new RatioGauge() {
			@Override
			public Ratio getRatio() {
				return cacheUsageListener.getHitRatio1Min();
			}
		});

		metrics.put(name("size_count").tag("cache_name", cacheName).build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return cache.getLiveCacheStatistics().getSize();
			}
		});

		metrics.put(name("bytes_used").tag("cache_name", cacheName).build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return cache.getLiveCacheStatistics().getLocalDiskSizeInBytes() +
						cache.getLiveCacheStatistics().getLocalHeapSizeInBytes() +
						cache.getLiveCacheStatistics().getLocalOffHeapSizeInBytes();
			}
		});

		return metrics;
	}

}
