package org.stagemonitor.tracing.metrics;

import org.stagemonitor.core.util.TimeUtils;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.tracing.wrapper.StatelessSpanEventListener;

import java.util.concurrent.TimeUnit;

public class CpuTimeTrackingSpanEventListener extends StatelessSpanEventListener {

	private static final double MILLISECOND_IN_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

	private final static String CPU_TIME_ATTRIBUTE = CpuTimeTrackingSpanEventListener.class.getName() + ".cpuTime";

	@Override
	public void onStart(SpanWrapper spanWrapper) {
		final SpanContextInformation spanContextInformation = SpanContextInformation.forSpan(spanWrapper);
		if (!SpanUtils.isExternalRequest(spanWrapper)) {
			spanContextInformation.addRequestAttribute(CPU_TIME_ATTRIBUTE, TimeUtils.getCpuTime());
		}
	}

	@Override
	public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
		Long startCpu = SpanContextInformation.forSpan(spanWrapper).getRequestAttribute(CPU_TIME_ATTRIBUTE);
		if (startCpu != null) {
			final long cpuTime = TimeUtils.getCpuTime() - startCpu;
			spanWrapper.setTag("duration_cpu_ms", cpuTime / MILLISECOND_IN_NANOS);
		}
	}
}
