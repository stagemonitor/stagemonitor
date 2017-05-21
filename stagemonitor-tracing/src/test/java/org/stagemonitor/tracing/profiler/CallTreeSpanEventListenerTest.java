package org.stagemonitor.tracing.profiler;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.tracing.RequestMonitor;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.sampling.PostExecutionInterceptorContext;
import org.stagemonitor.tracing.sampling.PreExecutionInterceptorContext;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CallTreeSpanEventListenerTest {

	private TracingPlugin tracingPlugin;
	private SpanWrapper span;

	@Before
	public void setUp() throws Exception {
		tracingPlugin = mock(TracingPlugin.class);

		when(tracingPlugin.getProfilerRateLimitPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(tracingPlugin.isProfilerActive()).thenReturn(true);
		final RequestMonitor requestMonitor = mock(RequestMonitor.class);
		doReturn(requestMonitor).when(tracingPlugin).getRequestMonitor();
		span = mock(SpanWrapper.class);
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
		verify(span).setTag(eq(SpanUtils.CALL_TREE_JSON), anyString());
		verify(span).setTag(eq(SpanUtils.CALL_TREE_ASCII), anyString());
	}

	@Test
	public void testExcludeCallTreeTags() throws Exception {
		doReturn(1000000d).when(tracingPlugin).getProfilerRateLimitPerMinute();
		when(tracingPlugin.getExcludedTags()).thenReturn(Arrays.asList(SpanUtils.CALL_TREE_JSON, SpanUtils.CALL_TREE_ASCII));
		final SpanContextInformation spanContext = invokeEventListener();
		assertNotNull(spanContext.getCallTree());
		verify(span, never()).setTag(eq(SpanUtils.CALL_TREE_JSON), anyString());
		verify(span, never()).setTag(eq(SpanUtils.CALL_TREE_ASCII), anyString());
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
		final SpanContextInformation contextInformation = SpanContextInformation.forSpan(span);
		contextInformation.setSampled(sampled);
		contextInformation.setPreExecutionInterceptorContext(new PreExecutionInterceptorContext(contextInformation));
		contextInformation.setPostExecutionInterceptorContext(new PostExecutionInterceptorContext(contextInformation));
		eventListener.onStart(span);
		eventListener.onFinish(span, "test", 0);
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
