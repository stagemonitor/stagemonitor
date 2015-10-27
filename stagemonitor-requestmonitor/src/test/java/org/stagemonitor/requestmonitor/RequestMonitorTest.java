package org.stagemonitor.requestmonitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;

public class RequestMonitorTest {

	private CorePlugin corePlugin = mock(CorePlugin.class);
	private RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
	private MetricRegistry registry = mock(MetricRegistry.class);
	private RequestMonitor requestMonitor;

	@Before
	public void before() {
		Stagemonitor.reset();
		SharedMetricRegistries.clear();
		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		when(requestMonitorPlugin.isCollectRequestStats()).thenReturn(true);
		when(requestMonitorPlugin.isProfilerActive()).thenReturn(true);
		when(registry.timer(anyString())).thenReturn(mock(Timer.class));
		when(registry.meter(anyString())).thenReturn(mock(Meter.class));
		requestMonitor = new RequestMonitor(corePlugin, registry, requestMonitorPlugin);
	}

	@After
	public void after() {
		Stagemonitor.reset();
		SharedMetricRegistries.clear();
	}

	@Test
	public void testDeactivated() throws Exception {
		when(corePlugin.isStagemonitorActive()).thenReturn(false);

		final RequestMonitor.RequestInformation requestInformation = requestMonitor.monitor(createMonitoredRequest());

		assertEquals("test", requestInformation.getExecutionResult());
		assertNull(requestInformation.getRequestTrace());
	}

	@Test
	public void testNotWarmedUp() throws Exception {
		when(requestMonitorPlugin.getNoOfWarmupRequests()).thenReturn(2);
		requestMonitor = new RequestMonitor(corePlugin, registry, requestMonitorPlugin);
		final RequestMonitor.RequestInformation requestInformation = requestMonitor.monitor(createMonitoredRequest());
		assertNull(requestInformation.getRequestTrace());
	}

	@Test
	public void testRecordException() throws Exception {
		final MonitoredRequest<RequestTrace> monitoredRequest = createMonitoredRequest();
		when(monitoredRequest.execute()).thenThrow(new RuntimeException("test"));

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				RequestMonitor.RequestInformation<?> requestInformation = (RequestMonitor.RequestInformation) invocation.getArguments()[0];
				assertEquals("java.lang.RuntimeException", requestInformation.getRequestTrace().getExceptionClass());
				assertEquals("test", requestInformation.getRequestTrace().getExceptionMessage());
				assertNotNull(requestInformation.getRequestTrace().getExceptionStackTrace());
				return null;
			}
		}).when(monitoredRequest).onPostExecute(Mockito.<RequestMonitor.RequestInformation<RequestTrace>>any());

		try {
			requestMonitor.monitor(monitoredRequest);
		} catch (Exception e) {
		}
	}

	@Test
	public void testInternalMetricsDeactive() throws Exception {
		internalMonitoringTestHelper(false);
	}

	@Test
	public void testInternalMetricsActive() throws Exception {
		when(corePlugin.isInternalMonitoringActive()).thenReturn(true);

		requestMonitor.monitor(createMonitoredRequest());
		verify(registry, times(0)).timer("internal.overhead.RequestMonitor");

		requestMonitor.monitor(createMonitoredRequest());
		verify(registry, times(1)).timer("internal.overhead.RequestMonitor");
	}

	private void internalMonitoringTestHelper(boolean active) throws Exception {
		when(corePlugin.isInternalMonitoringActive()).thenReturn(active);
		requestMonitor.monitor(createMonitoredRequest());
		verify(registry, times(active ? 1 : 0)).timer("internal.overhead.RequestMonitor");
	}

	private MonitoredRequest<RequestTrace> createMonitoredRequest() throws Exception {
		@SuppressWarnings("unchecked")
		final MonitoredRequest<RequestTrace> monitoredRequest = mock(MonitoredRequest.class);
		when(monitoredRequest.execute()).thenReturn("test");
		when(monitoredRequest.createRequestTrace()).thenReturn(new RequestTrace(null, new RequestTrace.GetNameCallback() {
			@Override
			public String getName() {
				return "test";
			}
		}));
		return monitoredRequest;
	}

	@Test
	public void testProfileThisExecutionDeactive() throws Exception {
		when(requestMonitorPlugin.getCallStackEveryXRequestsToGroup()).thenReturn(0);
		final RequestMonitor.RequestInformation<RequestTrace> monitor = requestMonitor.monitor(createMonitoredRequest());
		assertNull(monitor.getRequestTrace().getCallStack());
	}

	@Test
	public void testProfileThisExecutionAlwaysActive() throws Exception {
		when(requestMonitorPlugin.getCallStackEveryXRequestsToGroup()).thenReturn(1);
		final RequestMonitor.RequestInformation<RequestTrace> monitor = requestMonitor.monitor(createMonitoredRequest());
		assertNotNull(monitor.getRequestTrace().getCallStack());
	}

	@Test
	public void testProfileThisExecutionActiveEvery2Requests() throws Exception {
		RequestMonitor.addRequestTraceReporter(new RequestTraceReporter() {
			@Override
			public <T extends RequestTrace> void reportRequestTrace(T requestTrace) throws Exception {
			}
			@Override
			public <T extends RequestTrace> boolean isActive(T requestTrace) {
				return true;
			}
		});
		testProfileThisExecutionHelper(2, 0, false);
		testProfileThisExecutionHelper(2, 1, false);
		testProfileThisExecutionHelper(2, 2, true);
		testProfileThisExecutionHelper(2, 3, false);
		testProfileThisExecutionHelper(2, 4, true);
	}

	private void testProfileThisExecutionHelper(int callStackEveryXRequestsToGroup, long timerCount, boolean callStackExpected) throws Exception {
		when(requestMonitorPlugin.getCallStackEveryXRequestsToGroup()).thenReturn(callStackEveryXRequestsToGroup);
		final Timer timer = mock(Timer.class);
		when(timer.getCount()).thenReturn(timerCount);
		when(registry.timer("request.test.server.time.total")).thenReturn(timer);

		final RequestMonitor.RequestInformation<RequestTrace> monitor = requestMonitor.monitor(createMonitoredRequest());
		if (callStackExpected) {
			assertNotNull(monitor.getRequestTrace().getCallStack());
		} else {
			assertNull(monitor.getRequestTrace().getCallStack());
		}
	}

	@Test
	public void testGetInstanceNameFromExecution() throws Exception {
		final MonitoredRequest<RequestTrace> monitoredRequest = createMonitoredRequest();
		when(monitoredRequest.getInstanceName()).thenReturn("testInstance");
		requestMonitor.monitor(monitoredRequest);
		assertEquals("testInstance", Stagemonitor.getMeasurementSession().getInstanceName());
	}
}
