package org.stagemonitor.ehcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import org.junit.Test;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

public class StagemonitorCacheUsageListenerTest {

	private final Metric2Registry registry = new Metric2Registry();
	private StagemonitorCacheUsageListener cacheUsageListener = new StagemonitorCacheUsageListener("cache", registry, true);

	@Test
	public void testEvicted() throws Exception {
		cacheUsageListener.notifyCacheElementEvicted();

		final MetricName name = name("cache_delete").tag("cache_name", "cache").tag("reason", "eviction").tier("All").build();
		assertNotNull(registry.getMeters().get(name));
		assertEquals(1, registry.getMeters().get(name).getCount());
	}

	@Test
	public void testExpired() throws Exception {
		cacheUsageListener.notifyCacheElementExpired();

		final MetricName name = name("cache_delete").tag("cache_name", "cache").tag("reason", "expire").tier("All").build();
		assertNotNull(registry.getMeters().get(name));
		assertEquals(1, registry.getMeters().get(name).getCount());
	}

	@Test
	public void testRemoved() throws Exception {
		cacheUsageListener.notifyCacheElementRemoved();

		final MetricName name = name("cache_delete").tag("cache_name", "cache").tag("reason", "remove").tier("All").build();
		assertNotNull(registry.getMeters().get(name));
		assertEquals(1, registry.getMeters().get(name).getCount());
	}

	@Test
	public void testCacheHit() throws Exception {
		cacheUsageListener.notifyCacheHitInMemory();
		cacheUsageListener.notifyCacheHitOffHeap();
		cacheUsageListener.notifyCacheHitOnDisk();

		final MetricName name = name("cache_hits").tag("cache_name", "cache").tier("All").build();
		assertNotNull(registry.getMeters().get(name));
		assertEquals(3, registry.getMeters().get(name).getCount());
	}

	@Test
	public void testCacheMiss() throws Exception {
		cacheUsageListener.notifyCacheMissedWithExpired();
		cacheUsageListener.notifyCacheMissedWithNotFound();
		cacheUsageListener.notifyCacheMissInMemory();
		cacheUsageListener.notifyCacheMissOffHeap();
		cacheUsageListener.notifyCacheMissOnDisk();

		final MetricName name = name("cache_misses").tag("cache_name", "cache").tier("All").build();
		assertNotNull(registry.getMeters().get(name));
		assertEquals(5, registry.getMeters().get(name).getCount());
	}

	@Test
	public void testHitRate() throws Exception {
		cacheUsageListener.notifyCacheHitInMemory();
		cacheUsageListener.notifyCacheMissOnDisk();
		assertNotNull(cacheUsageListener.getHitRatio1Min());
	}

	@Test
	public void testGet() {
		cacheUsageListener.notifyGetTimeNanos(1);
		assertNotNull(registry.getTimers().get(name("cache_get").tag("cache_name", "cache").tier("All").build()));
	}

	@Test
	public void testGetMeter() {
		cacheUsageListener = new StagemonitorCacheUsageListener("cache", registry, false);
		cacheUsageListener.notifyGetTimeNanos(1);

		final MetricName name = name("cache_get").tag("cache_name", "cache").tier("All").build();
		assertNull(registry.getTimers().get(name));
		assertNotNull(registry.getMeters().get(name));
	}

	@Test
	public void testEmpty() {
		cacheUsageListener.notifyRemoveAll();
		cacheUsageListener.notifyStatisticsAccuracyChanged(0);
		cacheUsageListener.dispose();
		cacheUsageListener.notifyCacheSearch(0);
		cacheUsageListener.notifyXaCommit();
		cacheUsageListener.notifyXaRollback();
		cacheUsageListener.notifyStatisticsEnabledChanged(true);
		cacheUsageListener.notifyStatisticsCleared();
		cacheUsageListener.notifyCacheElementPut();
		cacheUsageListener.notifyCacheElementUpdated();
		cacheUsageListener.notifyTimeTakenForGet(0);

		assertEquals(0, registry.getMeters().size());
		assertEquals(0, registry.getCounters().size());
		assertEquals(0, registry.getTimers().size());
		assertEquals(0, registry.getGauges().size());
		assertEquals(0, registry.getHistograms().size());
	}
}
