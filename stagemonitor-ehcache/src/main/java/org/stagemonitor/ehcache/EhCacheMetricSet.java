package org.stagemonitor.ehcache;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.RatioGauge;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;

import java.util.HashMap;
import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * An instrumented {@link net.sf.ehcache.Ehcache} instance.
 */
public class EhCacheMetricSet implements MetricSet {

	private final String metricPrefix;
	private final Ehcache cache;
	private final StagemonitorCacheUsageListener cacheUsageListener;

	public EhCacheMetricSet(String metricPrefix, Cache cache, StagemonitorCacheUsageListener cacheUsageListener) {
		this.metricPrefix = metricPrefix;
		this.cache = cache;
		this.cacheUsageListener = cacheUsageListener;
	}

	@Override
	public Map<String, Metric> getMetrics() {
		final Map<String, Metric> metrics = new HashMap<String, Metric>();

		metrics.put(name(metricPrefix, "access.hit.total.ratio"), new RatioGauge() {
			@Override
			public Ratio getRatio() {
				return cacheUsageListener.getHitRatio1Min();
			}
		});

		metrics.put(name(metricPrefix, "size.count"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return cache.getLiveCacheStatistics().getSize();
			}
		});

		metrics.put(name(metricPrefix, "bytes.used"), new Gauge<Long>() {
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
