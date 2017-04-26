package org.stagemonitor.tracing.profiler;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.tracing.RequestMonitor;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.sampling.PostExecutionInterceptorContext;
import org.stagemonitor.tracing.sampling.PreExecutionInterceptorContext;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CallTreeSpanEventListenerTest {

	private TracingPlugin tracingPlugin;

	@Before
	public void setUp() throws Exception {
		tracingPlugin = mock(TracingPlugin.class);

		when(tracingPlugin.getProfilerRateLimitPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(tracingPlugin.isProfilerActive()).thenReturn(true);
		final RequestMonitor requestMonitor = mock(RequestMonitor.class);
		doReturn(requestMonitor).when(tracingPlugin).getRequestMonitor();
	}

	@Test
	public void testProfileThisExecutionDeactive() throws Exception {
		doReturn(0d).when(tracingPlugin).getProfilerRateLimitPerMinute();
		final SpanContextInformation spanContext = invokeEventListener();
		assertNull(spanContext.getCallTree());
	}

	@Test
	public void testProfileThisExecutionAlwaysActive() throws Exception {
		doReturn(1000000d).when(tracingPlugin).getProfilerRateLimitPerMinute();
		final SpanContextInformation spanContext = invokeEventListener();
		assertNotNull(spanContext.getCallTree());
	}

	@Test
	public void testDontActivateProfilerWhenSpanIsNotSampled() throws Exception {
		doReturn(1000000d).when(tracingPlugin).getProfilerRateLimitPerMinute();
		final SpanContextInformation spanContext = invokeEventListener(false);
		assertNull(spanContext.getCallTree());
	}

	private SpanContextInformation invokeEventListener() {
		return invokeEventListener(true);
	}

	private SpanContextInformation invokeEventListener(boolean sampled) {
		CallTreeSpanEventListener eventListener = new CallTreeSpanEventListener(tracingPlugin);
		final SpanWrapper span = mock(SpanWrapper.class);
		final SpanContextInformation contextInformation = SpanContextInformation.forSpan(span);
		contextInformation.setSampled(sampled);
		contextInformation.setPreExecutionInterceptorContext(new PreExecutionInterceptorContext(contextInformation));
		contextInformation.setPostExecutionInterceptorContext(new PostExecutionInterceptorContext(contextInformation));
		eventListener.onStart(span);
		eventListener.onFinish(span, "", 0);
		return contextInformation;
	}

	@Test
	public void testRateLimiting() throws Exception {
		when(tracingPlugin.getProfilerRateLimitPerMinute()).thenReturn(1d);
		final SpanContextInformation spanContext1 = invokeEventListener();
		assertNotNull(spanContext1.getCallTree());

		final SpanContextInformation spanContext2 = invokeEventListener();
		assertNotNull(spanContext2.getCallTree());

	}

}
