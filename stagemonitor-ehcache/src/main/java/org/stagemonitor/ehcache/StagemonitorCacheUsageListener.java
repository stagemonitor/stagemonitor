package org.stagemonitor.ehcache;

import com.codahale.metrics.Meter;

import net.sf.ehcache.statistics.CacheUsageListener;

import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;

import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.RatioGauge.Ratio;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class StagemonitorCacheUsageListener implements CacheUsageListener {

	private final Metric2Registry registry;
	private final boolean timeGet;
	private final MetricName allCacheHitsMetricName;
	private final MetricName allCacheMissesMetricName;
	private final MetricName getMetricName;
	private final MetricName deleteEvictionMetricName;
	private final MetricName deleteExpireMetricName;
	private final MetricName deleteRemovedMetricName;


	public StagemonitorCacheUsageListener(String cacheName, Metric2Registry registry, boolean timeGet) {
		this.registry = registry;
		this.timeGet = timeGet;
		allCacheHitsMetricName = name("cache_hits").tag("cache_name", cacheName).tier("All").build();
		allCacheMissesMetricName = name("cache_misses").tag("cache_name", cacheName).tier("All").build();
		getMetricName = name("cache_get").tag("cache_name", cacheName).tier("All").build();
		deleteEvictionMetricName = name("cache_delete").tag("cache_name", cacheName).tag("reason", "eviction").tier("All").build();
		deleteExpireMetricName = name("cache_delete").tag("cache_name", cacheName).tag("reason", "expire").tier("All").build();
		deleteRemovedMetricName = name("cache_delete").tag("cache_name", cacheName).tag("reason", "remove").tier("All").build();
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
		return Ratio.of(oneMinuteHitRate * 100.0, oneMinuteHitRate + missRate.getOneMinuteRate());
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
			registry.timer(getMetricName).update(nanos, TimeUnit.NANOSECONDS);
		} else {
			registry.meter(getMetricName).mark();
		}
	}

	@Override
	public void notifyCacheElementEvicted() {
		registry.meter(deleteEvictionMetricName).mark();
	}

	@Override
	public void notifyCacheElementExpired() {
		registry.meter(deleteExpireMetricName).mark();
	}

	@Override
	public void notifyCacheElementRemoved() {
		registry.meter(deleteRemovedMetricName).mark();
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
