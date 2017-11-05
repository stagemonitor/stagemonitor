package org.stagemonitor.tracing.sampling;

import org.junit.Test;
import org.stagemonitor.tracing.AbstractRequestMonitorTest;
import org.stagemonitor.tracing.MonitoredMethodRequest;

import io.opentracing.tag.Tags;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SamplePriorityDeterminingSpanEventListenerTest extends AbstractRequestMonitorTest {

	@Test
	public void testSetSamplePrioInPreInterceptor() throws Exception {
		when(tracingPlugin.isSampled(any())).thenReturn(true);
		samplePriorityDeterminingSpanInterceptor.addPreInterceptor(new PreExecutionSpanInterceptor() {
			@Override
			public void interceptReport(PreExecutionInterceptorContext context) {
				context.shouldNotReport(getClass());
			}
		});

		requestMonitor.monitor(new MonitoredMethodRequest(configuration,
				"testSetSamplePrioInPreInterceptor", () -> {
		}));
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		assertThat(mockTracer.finishedSpans().get(0).tags()).containsEntry(Tags.SAMPLING_PRIORITY.getKey(), 0);
	}
}
