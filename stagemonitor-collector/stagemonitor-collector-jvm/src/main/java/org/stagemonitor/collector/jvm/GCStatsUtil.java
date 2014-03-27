package org.stagemonitor.collector.jvm;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

public class GCStatsUtil {

	public static GCStats getGCStats() {
		long totalGarbageCollections = 0;
		long garbageCollectionTime = 0;

		for (GarbageCollectorMXBean gc :
				ManagementFactory.getGarbageCollectorMXBeans()) {

			long count = gc.getCollectionCount();

			if (count >= 0) {
				totalGarbageCollections += count;
			}

			long time = gc.getCollectionTime();

			if (time >= 0) {
				garbageCollectionTime += time;
			}
		}

		return new GCStats(garbageCollectionTime, totalGarbageCollections);
	}


	public static class GCStats {
		public final long collectionCount;
		public final long garbageCollectionTime;

		public GCStats(long garbageCollectionTime, long collectionCount) {
			this.garbageCollectionTime = garbageCollectionTime;
			this.collectionCount = collectionCount;
		}
	}
}
