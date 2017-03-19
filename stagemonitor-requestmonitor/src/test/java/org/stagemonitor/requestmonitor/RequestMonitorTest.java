package org.stagemonitor.requestmonitor;

import com.uber.jaeger.Span;
import com.uber.jaeger.context.TracingUtils;
import com.uber.jaeger.samplers.ConstSampler;

import org.junit.Test;
import org.mockito.Mockito;
import org.stagemonitor.requestmonitor.tracing.jaeger.LoggingSpanReporter;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.opentracing.Tracer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class RequestMonitorTest extends AbstractRequestMonitorTest {

	protected Tracer getTracer() {
		return new com.uber.jaeger.Tracer.Builder("RequestMonitorTest",
				new LoggingSpanReporter(requestMonitorPlugin), new ConstSampler(true)).build();
	}

	@Test
	public void testDeactivated() throws Exception {
		doReturn(false).when(corePlugin).isStagemonitorActive();

		final SpanContextInformation spanContext = requestMonitor.monitor(createMonitoredRequest());

		assertNull(spanContext);
	}

	@Test
	public void testRecordException() throws Exception {
		final MonitoredRequest monitoredRequest = createMonitoredRequest();
		doThrow(new RuntimeException("test")).when(monitoredRequest).execute();
		try {
			requestMonitor.monitor(monitoredRequest);
		} catch (Exception e) {
		}
		assertEquals("java.lang.RuntimeException", tags.get("exception.class"));
		assertEquals("test", tags.get("exception.message"));
		assertNotNull(tags.get("exception.stack_trace"));
	}

	@Test
	public void testInternalMetricsDeactive() throws Exception {
		internalMonitoringTestHelper(false);
	}

	@Test
	public void testInternalMetricsActive() throws Exception {
		doReturn(true).when(corePlugin).isInternalMonitoringActive();

		requestMonitor.monitor(createMonitoredRequest());
		verify(registry, times(1)).timer(name("internal_overhead_request_monitor").build());
	}

	private void internalMonitoringTestHelper(boolean active) throws Exception {
		doReturn(active).when(corePlugin).isInternalMonitoringActive();
		requestMonitor.monitor(createMonitoredRequest());
		verify(registry, times(active ? 1 : 0)).timer(name("internal_overhead_request_monitor").build());
	}

	private MonitoredRequest createMonitoredRequest() throws Exception {
		return Mockito.spy(new MonitoredMethodRequest(configuration, "test", () -> {
		}));
	}

	@Test
	public void testExecutorServiceContextPropagation() throws Exception {
		final ExecutorService executorService = TracingUtils.tracedExecutor(Executors.newSingleThreadExecutor());
		final SpanContextInformation[] asyncSpan = new SpanContextInformation[1];

		final Future<?>[] asyncMethodCallFuture = new Future<?>[1];
		final SpanContextInformation testInfo = requestMonitor.monitor(new MonitoredMethodRequest(configuration, "test", () -> {
			assertNotNull(TracingUtils.getTraceContext().getCurrentSpan());
			asyncMethodCallFuture[0] = monitorAsyncMethodCall(executorService, asyncSpan);
		}));
		executorService.shutdown();
		// waiting for completion
		spanCapturingReporter.get();
		spanCapturingReporter.get();
		asyncMethodCallFuture[0].get();
		assertEquals("test", testInfo.getOperationName());
		assertEquals("async", asyncSpan[0].getOperationName());

		assertEquals(((SpanWrapper) testInfo.getSpan()).unwrap(Span.class).context(), ((SpanWrapper) asyncSpan[0].getSpan()).unwrap(Span.class).context());
	}

	private Future<?> monitorAsyncMethodCall(ExecutorService executorService, final SpanContextInformation[] asyncSpan) {
		return executorService.submit((Callable<Object>) () ->
				asyncSpan[0] = requestMonitor.monitor(new MonitoredMethodRequest(configuration, "async", () -> {
					assertNotNull(TracingUtils.getTraceContext().getCurrentSpan());
					callAsyncMethod();
				})));
	}

	private Object callAsyncMethod() {
		return null;
	}

	@Test
	public void testDontMonitorClientRootSpans() throws Exception {
		when(requestMonitorPlugin.getRateLimitClientSpansPerMinute()).thenReturn(1_000_000.0);

		requestMonitor.monitorStart(new AbstractExternalRequest(requestMonitorPlugin) {
			@Override
			protected String getType() {
				return "jdbc";
			}
		});

		assertFalse(SpanContextInformation.getCurrent().isSampled());

		requestMonitor.monitorStop();
	}
}
