package org.stagemonitor.tracing.metrics;

import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.tracing.wrapper.StatelessSpanEventListener;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

public class GcTimeTrackingSpanEventListener extends StatelessSpanEventListener {

	private static final String GC_TIME_ATTRIBUTE = GcTimeTrackingSpanEventListener.class.getName() + ".gcTime";

	private static long collectGcTimeMs() {
		final List<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
		long sum = 0L;

		for (GarbageCollectorMXBean bean : garbageCollectorMXBeans) {
			sum += bean.getCollectionTime();
		}

		return sum;
	}

	@Override
	public void onStart(SpanWrapper spanWrapper) {
		SpanContextInformation.forSpan(spanWrapper).addRequestAttribute(GC_TIME_ATTRIBUTE, collectGcTimeMs());
	}

	@Override
	public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
		Long startGcTime = SpanContextInformation.forSpan(spanWrapper).getRequestAttribute(GC_TIME_ATTRIBUTE);
		if (startGcTime != null) {
			spanWrapper.setTag("gc_time_ms", collectGcTimeMs() - startGcTime);
		}
	}
}
