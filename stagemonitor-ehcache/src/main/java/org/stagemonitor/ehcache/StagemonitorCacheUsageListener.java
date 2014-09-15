package org.stagemonitor.ehcache;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import net.sf.ehcache.statistics.CacheUsageListener;

import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static com.codahale.metrics.RatioGauge.Ratio;

public class StagemonitorCacheUsageListener implements CacheUsageListener {

	private static final String DELETE = "delete";
	private final String metricPrefix;
	private final MetricRegistry registry;
	private final boolean timeGet;
	final String allCacheHitsMetricName;
	final String allCacheMissesMetricName;


	public StagemonitorCacheUsageListener(String metricPrefix, MetricRegistry registry, boolean timeGet) {
		this.metricPrefix = metricPrefix;
		this.registry = registry;
		this.timeGet = timeGet;
		allCacheHitsMetricName = name(metricPrefix, "access.hit.total");
		allCacheMissesMetricName = name(metricPrefix, "access.miss.total");
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
			registry.timer(name(metricPrefix, "get")).update(nanos, TimeUnit.NANOSECONDS);
		} else {
			registry.meter(name(metricPrefix, "get")).mark();
		}
	}

	@Override
	public void notifyCacheElementEvicted() {
		registry.meter(name(metricPrefix, DELETE, "eviction")).mark();
	}

	@Override
	public void notifyCacheElementExpired() {
		registry.meter(name(metricPrefix, DELETE, "expire")).mark();
	}

	@Override
	public void notifyCacheElementRemoved() {
		registry.meter(name(metricPrefix, DELETE, "remove")).mark();
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
