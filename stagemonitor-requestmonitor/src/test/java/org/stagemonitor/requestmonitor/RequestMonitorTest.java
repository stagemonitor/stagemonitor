package org.stagemonitor.requestmonitor;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.StageMonitor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RequestMonitorTest {

	private Configuration configuration = mock(Configuration.class);
	private MetricRegistry registry = mock(MetricRegistry.class);
	private RequestMonitor requestMonitor = new RequestMonitor(configuration, registry);

	@Before
	public void before() {
		StageMonitor.reset();
		when(configuration.isStagemonitorActive()).thenReturn(true);
		when(configuration.isCollectRequestStats()).thenReturn(true);
		when(registry.timer(anyString())).thenReturn(mock(Timer.class));
		when(registry.meter(anyString())).thenReturn(mock(Meter.class));
	}

	@After
	public void after() {
		StageMonitor.reset();
	}

	@Test
	public void testDeactivated() throws Exception {
		when(configuration.isStagemonitorActive()).thenReturn(false);

		final RequestMonitor.RequestInformation requestInformation = requestMonitor.monitor(createMonitoredRequest());

		assertEquals("test", requestInformation.getExecutionResult());
		assertNull(requestInformation.getRequestTrace());
	}

	@Test
	public void testNotWarmedUp() throws Exception {
		when(configuration.getNoOfWarmupRequests()).thenReturn(2);
		requestMonitor = new RequestMonitor(configuration, registry);
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
		internalMonitoringTestHelper(true);
	}

	private void internalMonitoringTestHelper(boolean active) throws Exception {
		when(configuration.isInternalMonitoringActive()).thenReturn(active);
		requestMonitor.monitor(createMonitoredRequest());
		verify(registry, times(active ? 1 : 0)).timer("internal.overhead.RequestMonitor");
	}

	private MonitoredRequest<RequestTrace> createMonitoredRequest() throws Exception {
		@SuppressWarnings("unchecked")
		final MonitoredRequest<RequestTrace> monitoredRequest = mock(MonitoredRequest.class);
		when(monitoredRequest.execute()).thenReturn("test");
		when(monitoredRequest.createRequestTrace()).thenReturn(new RequestTrace(new RequestTrace.GetNameCallback() {
			@Override
			public String getName() {
				return "test";
			}
		}));
		return monitoredRequest;
	}

	@Test
	public void testProfileThisExecutionDeactive() throws Exception {
		when(configuration.getCallStackEveryXRequestsToGroup()).thenReturn(0);
		final RequestMonitor.RequestInformation<RequestTrace> monitor = requestMonitor.monitor(createMonitoredRequest());
		assertNull(monitor.getRequestTrace().getCallStack());
	}

	@Test
	public void testProfileThisExecutionAlwaysActive() throws Exception {
		when(configuration.getCallStackEveryXRequestsToGroup()).thenReturn(1);
		final RequestMonitor.RequestInformation<RequestTrace> monitor = requestMonitor.monitor(createMonitoredRequest());
		assertNotNull(monitor.getRequestTrace().getCallStack());
	}

	@Test
	public void testProfileThisExecutionActiveEvery2Requests() throws Exception {
		testProfileThisExecutionHelper(2, 0, false);
		testProfileThisExecutionHelper(2, 1, false);
		testProfileThisExecutionHelper(2, 2, true);
		testProfileThisExecutionHelper(2, 3, false);
		testProfileThisExecutionHelper(2, 4, true);
	}

	private void testProfileThisExecutionHelper(int callStackEveryXRequestsToGroup, long timerCount, boolean callStackExpected) throws Exception {
		when(configuration.getCallStackEveryXRequestsToGroup()).thenReturn(callStackEveryXRequestsToGroup);
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
		assertEquals("testInstance", StageMonitor.getMeasurementSession().getInstanceName());
	}
}
