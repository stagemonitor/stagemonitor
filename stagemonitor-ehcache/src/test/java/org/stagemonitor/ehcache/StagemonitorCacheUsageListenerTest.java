package org.stagemonitor.ehcache;

import com.codahale.metrics.MetricRegistry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class StagemonitorCacheUsageListenerTest {

	private final MetricRegistry registry = new MetricRegistry();
	private StagemonitorCacheUsageListener cacheUsageListener = new StagemonitorCacheUsageListener("cache", registry, true);

	@Test
	public void testEvicted() throws Exception {
		cacheUsageListener.notifyCacheElementEvicted();
		assertNotNull(registry.getMeters().get("cache.delete.eviction"));
		assertEquals(1, registry.getMeters().get("cache.delete.eviction").getCount());
	}

	@Test
	public void testExpired() throws Exception {
		cacheUsageListener.notifyCacheElementExpired();
		assertNotNull(registry.getMeters().get("cache.delete.expire"));
		assertEquals(1, registry.getMeters().get("cache.delete.expire").getCount());
	}

	@Test
	public void testRemoved() throws Exception {
		cacheUsageListener.notifyCacheElementRemoved();

		assertNotNull(registry.getMeters().get("cache.delete.remove"));
		assertEquals(1, registry.getMeters().get("cache.delete.remove").getCount());
	}

	@Test
	public void testCacheHit() throws Exception {
		cacheUsageListener.notifyCacheHitInMemory();
		cacheUsageListener.notifyCacheHitOffHeap();
		cacheUsageListener.notifyCacheHitOnDisk();

		assertNotNull(registry.getMeters().get("cache.access.hit.total"));
		assertEquals(3, registry.getMeters().get("cache.access.hit.total").getCount());
	}

	@Test
	public void testCacheMiss() throws Exception {
		cacheUsageListener.notifyCacheMissedWithExpired();
		cacheUsageListener.notifyCacheMissedWithNotFound();
		cacheUsageListener.notifyCacheMissInMemory();
		cacheUsageListener.notifyCacheMissOffHeap();
		cacheUsageListener.notifyCacheMissOnDisk();

		assertNotNull(registry.getMeters().get("cache.access.miss.total"));
		assertEquals(5, registry.getMeters().get("cache.access.miss.total").getCount());
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
		assertNotNull(registry.getTimers().get("cache.get"));
	}

	@Test
	public void testGetMeter() {
		cacheUsageListener = new StagemonitorCacheUsageListener("cache", registry, false);
		cacheUsageListener.notifyGetTimeNanos(1);
		assertNull(registry.getTimers().get("cache.get"));
		assertNotNull(registry.getMeters().get("cache.get"));
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
