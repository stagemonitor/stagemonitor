package org.stagemonitor.requestmonitor.profiler;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.sampling.PostExecutionInterceptorContext;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CallTreeSpanEventListenerTest {

	private RequestMonitorPlugin requestMonitorPlugin;
	private SpanContextInformation spanContext;

	@Before
	public void setUp() throws Exception {
		requestMonitorPlugin = mock(RequestMonitorPlugin.class);

		when(requestMonitorPlugin.getProfilerRateLimitPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(requestMonitorPlugin.isProfilerActive()).thenReturn(true);
		final RequestMonitor requestMonitor = mock(RequestMonitor.class);
		when(requestMonitor.getSpanContext()).then(invocation -> spanContext);
		doReturn(requestMonitor).when(requestMonitorPlugin).getRequestMonitor();
		initSpanContext();
	}

	public void initSpanContext() {
		spanContext = new SpanContextInformation();
		spanContext.setPostExecutionInterceptorContext(new PostExecutionInterceptorContext(mock(Configuration.class), spanContext, mock(Metric2Registry.class)));
	}

	@Test
	public void testProfileThisExecutionDeactive() throws Exception {
		doReturn(0d).when(requestMonitorPlugin).getProfilerRateLimitPerMinute();
		invokeEventListener();
		assertNull(spanContext.getCallTree());
	}

	@Test
	public void testProfileThisExecutionAlwaysActive() throws Exception {
		doReturn(1000000d).when(requestMonitorPlugin).getProfilerRateLimitPerMinute();
		invokeEventListener();
		assertNotNull(spanContext.getCallTree());
	}

	@Test
	public void testDontActivateProfilerWhenSpanIsNotSampled() throws Exception {
		spanContext.setReport(false);
		invokeEventListener();
		assertNull(spanContext.getCallTree());
	}

	private void invokeEventListener() {
		CallTreeSpanEventListener eventListener = new CallTreeSpanEventListener(requestMonitorPlugin);
		eventListener.onStart(mock(SpanWrapper.class));
		eventListener.onFinish(mock(SpanWrapper.class), "", 0);
	}

	@Test
	public void testRateLimiting() throws Exception {
		when(requestMonitorPlugin.getProfilerRateLimitPerMinute()).thenReturn(1d);
		invokeEventListener();
		assertNotNull(spanContext.getCallTree());

		initSpanContext();
		invokeEventListener();
		assertNotNull(spanContext.getCallTree());

	}

}
