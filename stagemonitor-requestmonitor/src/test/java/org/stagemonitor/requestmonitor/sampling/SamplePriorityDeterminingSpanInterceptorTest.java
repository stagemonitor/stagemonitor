package org.stagemonitor.requestmonitor.sampling;

import org.junit.Test;
import org.stagemonitor.requestmonitor.AbstractRequestMonitorTest;
import org.stagemonitor.requestmonitor.MonitoredMethodRequest;
import org.stagemonitor.requestmonitor.reporter.SpanReporter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SamplePriorityDeterminingSpanInterceptorTest extends AbstractRequestMonitorTest {

	@Test
	public void testSetSamplePrioInPreInterceptor() throws Exception {
		final SpanReporter spanReporter = mock(SpanReporter.class);
		when(spanReporter.isActive(any())).thenReturn(true);
		requestMonitor.addReporter(spanReporter);
		samplePriorityDeterminingSpanInterceptor.addPreInterceptor(new PreExecutionSpanReporterInterceptor() {
			@Override
			public void interceptReport(PreExecutionInterceptorContext context) {
				context.shouldNotReport(getClass());
			}
		});

		requestMonitor.monitor(new MonitoredMethodRequest(configuration, "testSetSamplePrioInPreInterceptor", () -> null));

		verify(spanReporter, never()).report(any());
	}
}
