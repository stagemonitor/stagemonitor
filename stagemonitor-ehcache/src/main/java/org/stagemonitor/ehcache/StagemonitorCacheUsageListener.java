package org.stagemonitor.ehcache;

import static com.codahale.metrics.RatioGauge.Ratio;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Meter;
import net.sf.ehcache.statistics.CacheUsageListener;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

public class StagemonitorCacheUsageListener implements CacheUsageListener {

	private final String cacheName;
	private final Metric2Registry registry;
	private final boolean timeGet;
	final MetricName allCacheHitsMetricName;
	final MetricName allCacheMissesMetricName;


	public StagemonitorCacheUsageListener(String cacheName, Metric2Registry registry, boolean timeGet) {
		this.cacheName = cacheName;
		this.registry = registry;
		this.timeGet = timeGet;
		allCacheHitsMetricName = name("total_cache_hits").tag("cache_name", cacheName).build();
		allCacheMissesMetricName = name("total_cache_misses").tag("cache_name", cacheName).build();
	}

	@Override
	public void notifyStatisticsEnabledChanged(boolean enableStatistics) {
	}

	@Override
	public void notifyStatisticsCleared() {
	}

	@Override
	public void notifyCacheHitInMemory() {
		notifyAllCacheHits();
	}

	@Override
	public void notifyCacheHitOffHeap() {
		notifyAllCacheHits();
	}

	@Override
	public void notifyCacheHitOnDisk() {
		notifyAllCacheHits();
	}

	@Override
	public void notifyCacheElementPut() {
	}

	@Override
	public void notifyCacheElementUpdated() {
	}

	@Override
	public void notifyCacheMissedWithNotFound() {
		notifyAllCacheMisses();
	}

	@Override
	public void notifyCacheMissedWithExpired() {
		notifyAllCacheMisses();
	}

	private void notifyAllCacheHits() {
		registry.meter(allCacheHitsMetricName).mark();
	}

	private void notifyAllCacheMisses() {
		registry.meter(allCacheMissesMetricName).mark();
	}

	public Ratio getHitRatio1Min() {
		final Meter hitRate = registry.meter(allCacheHitsMetricName);
		final Meter missRate = registry.meter(allCacheMissesMetricName);
		final double oneMinuteHitRate = hitRate.getOneMinuteRate();
		return Ratio.of(oneMinuteHitRate, oneMinuteHitRate + missRate.getOneMinuteRate());
	}

	@Override
	public void notifyCacheMissInMemory() {
		notifyAllCacheMisses();
	}

	@Override
	public void notifyCacheMissOffHeap() {
		notifyAllCacheMisses();
	}

	@Override
	public void notifyCacheMissOnDisk() {
		notifyAllCacheMisses();
	}

	@Override
	@Deprecated
	public void notifyTimeTakenForGet(long millis) {
	}

	@Override
	public void notifyGetTimeNanos(long nanos) {
		if (timeGet) {
			registry.timer(name("cache_get").tag("cache_name", cacheName).build()).update(nanos, TimeUnit.NANOSECONDS);
		} else {
			registry.meter(name("cache_get").tag("cache_name", cacheName).build()).mark();
		}
	}

	@Override
	public void notifyCacheElementEvicted() {
		registry.meter(name("cache_delete").tag("cache_name", cacheName).tag("reason", "eviction").build()).mark();
	}

	@Override
	public void notifyCacheElementExpired() {
		registry.meter(name("cache_delete").tag("cache_name", cacheName).tag("reason", "expire").build()).mark();
	}

	@Override
	public void notifyCacheElementRemoved() {
		registry.meter(name("cache_delete").tag("cache_name", cacheName).tag("reason", "remove").build()).mark();
	}

	@Override
	public void notifyRemoveAll() {
	}

	@Override
	public void notifyStatisticsAccuracyChanged(int statisticsAccuracy) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public void notifyCacheSearch(long executeTime) {
	}

	@Override
	public void notifyXaCommit() {
	}

	@Override
	public void notifyXaRollback() {
	}
}
