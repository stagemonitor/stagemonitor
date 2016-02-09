package org.stagemonitor.requestmonitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.Collections;

import com.codahale.metrics.Meter;
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
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;


public class RequestMonitorTest {

	private CorePlugin corePlugin;
	private RequestMonitorPlugin requestMonitorPlugin;
	private Metric2Registry registry;
	private RequestMonitor requestMonitor;
	private Configuration configuration;

	@Before
	public void before() {
		requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		configuration = mock(Configuration.class);
		corePlugin = mock(CorePlugin.class);

		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);

		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		when(corePlugin.getElasticsearchUrls()).thenReturn(Collections.singletonList("http://mockhost:9200"));
		when(corePlugin.getElasticsearchClient()).thenReturn(mock(ElasticsearchClient.class));
		when(requestMonitorPlugin.isCollectRequestStats()).thenReturn(true);
		when(requestMonitorPlugin.isProfilerActive()).thenReturn(true);
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(1000000d);
		registry = mock(Metric2Registry.class);
		when(registry.timer(any(MetricName.class))).thenReturn(mock(Timer.class));
		when(registry.meter(any(MetricName.class))).thenReturn(mock(Meter.class));
		requestMonitor = new RequestMonitor(configuration, registry);
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
		when(corePlugin.isInternalMonitoringActive()).thenReturn(true);

		requestMonitor.monitor(createMonitoredRequest());
		verify(registry, times(0)).timer(name("internal_overhead_request_monitor").build());

		requestMonitor.monitor(createMonitoredRequest());
		verify(registry, times(1)).timer(name("internal_overhead_request_monitor").build());
	}

	private void internalMonitoringTestHelper(boolean active) throws Exception {
		when(corePlugin.isInternalMonitoringActive()).thenReturn(active);
		requestMonitor.monitor(createMonitoredRequest());
		verify(registry, times(active ? 1 : 0)).timer(name("internal_overhead_request_monitor").build());
	}

	private MonitoredRequest<RequestTrace> createMonitoredRequest() throws Exception {
		@SuppressWarnings("unchecked")
		final MonitoredRequest<RequestTrace> monitoredRequest = mock(MonitoredRequest.class);
		when(monitoredRequest.execute()).thenReturn("test");
		final RequestTrace requestTrace = new RequestTrace("1");
		requestTrace.setName("test");
		when(monitoredRequest.createRequestTrace()).thenReturn(requestTrace);

		return monitoredRequest;
	}

	@Test
	public void testProfileThisExecutionDeactive() throws Exception {
		when(requestMonitorPlugin.getOnlyCollectNCallTreesPerMinute()).thenReturn(0d);
		final RequestMonitor.RequestInformation<RequestTrace> monitor = requestMonitor.monitor(createMonitoredRequest());
		assertNull(monitor.getRequestTrace().getCallStack());
	}

	@Test
	public void testProfileThisExecutionAlwaysActive() throws Exception {
		when(requestMonitorPlugin.getOnlyCollectNCallTreesPerMinute()).thenReturn(1000000d);
		final RequestMonitor.RequestInformation<RequestTrace> monitor = requestMonitor.monitor(createMonitoredRequest());
		assertNotNull(monitor.getRequestTrace().getCallStack());
	}

	@Test
	public void testProfileThisExecutionNotActiveWhenNoRequestTraceReporterIsActive() throws Exception {
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(0d);
		when(requestMonitorPlugin.getOnlyCollectNCallTreesPerMinute()).thenReturn(1000000d);
		final RequestMonitor.RequestInformation<RequestTrace> monitor = requestMonitor.monitor(createMonitoredRequest());
		assertNull(monitor.getRequestTrace().getCallStack());
	}

	@Test
	public void testProfileThisExecutionActiveEvery2Requests() throws Exception {
		testProfileThisExecutionHelper(2, 0, true);
		testProfileThisExecutionHelper(2, 1.99, true);
		testProfileThisExecutionHelper(2, 2, false);
		testProfileThisExecutionHelper(2, 3, false);
		testProfileThisExecutionHelper(2, 1, true);
	}

	private void testProfileThisExecutionHelper(double onlyCollectNCallTreesPerMinute, double callTreeRate, boolean callStackExpected) throws Exception {
		when(requestMonitorPlugin.getOnlyCollectNCallTreesPerMinute()).thenReturn(onlyCollectNCallTreesPerMinute);
		final Meter callTreeMeter = mock(Meter.class);
		when(callTreeMeter.getOneMinuteRate()).thenReturn(callTreeRate);
		requestMonitor.setCallTreeMeter(callTreeMeter);

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
