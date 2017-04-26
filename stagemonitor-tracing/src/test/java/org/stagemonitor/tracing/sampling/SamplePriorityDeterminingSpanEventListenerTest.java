package org.stagemonitor.tracing.sampling;

import org.junit.Test;
import org.stagemonitor.tracing.AbstractRequestMonitorTest;
import org.stagemonitor.tracing.MonitoredMethodRequest;
import org.stagemonitor.tracing.SpanContextInformation;

import static org.junit.Assert.assertFalse;

public class SamplePriorityDeterminingSpanEventListenerTest extends AbstractRequestMonitorTest {

	@Test
	public void testSetSamplePrioInPreInterceptor() throws Exception {
		samplePriorityDeterminingSpanInterceptor.addPreInterceptor(new PreExecutionSpanInterceptor() {
			@Override
			public void interceptReport(PreExecutionInterceptorContext context) {
				context.shouldNotReport(getClass());
			}
		});

		final MonitoredMethodRequest monitoredRequest = new MonitoredMethodRequest(configuration,
				"testSetSamplePrioInPreInterceptor", () -> {
		});
		final SpanContextInformation info = requestMonitor.monitor(monitoredRequest);
		assertFalse(info.isSampled());
	}
}
