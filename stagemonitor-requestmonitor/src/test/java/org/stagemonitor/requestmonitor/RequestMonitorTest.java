package org.stagemonitor.requestmonitor;

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

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;


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
		registry = mock(Metric2Registry.class);

		doReturn(corePlugin).when(configuration).getConfig(CorePlugin.class);
		doReturn(requestMonitorPlugin).when(configuration).getConfig(RequestMonitorPlugin.class);

		doReturn(true).when(corePlugin).isStagemonitorActive();
		doReturn(1000).when(corePlugin).getThreadPoolQueueCapacityLimit();
		doReturn(Collections.singletonList("http://mockhost:9200")).when(corePlugin).getElasticsearchUrls();
		ElasticsearchClient elasticsearchClient = mock(ElasticsearchClient.class);
		doReturn(true).when(elasticsearchClient).isElasticsearchAvailable();
		doReturn(elasticsearchClient).when(corePlugin).getElasticsearchClient();
		doReturn(false).when(corePlugin).isOnlyLogElasticsearchMetricReports();

		doReturn(true).when(requestMonitorPlugin).isCollectRequestStats();
		doReturn(true).when(requestMonitorPlugin).isProfilerActive();
		doReturn(1000000d).when(requestMonitorPlugin).getOnlyReportNRequestsPerMinuteToElasticsearch();
		doReturn(mock(Timer.class)).when(registry).timer(any(MetricName.class));
		doReturn(mock(Meter.class)).when(registry).meter(any(MetricName.class));
		requestMonitor = new RequestMonitor(configuration, registry);
	}

	@After
	public void after() {
		Stagemonitor.reset();
		SharedMetricRegistries.clear();
	}

	@Test
	public void testDeactivated() throws Exception {
		doReturn(false).when(corePlugin).isStagemonitorActive();

		final RequestMonitor.RequestInformation requestInformation = requestMonitor.monitor(createMonitoredRequest());

		assertEquals("test", requestInformation.getExecutionResult());
		assertNull(requestInformation.getRequestTrace());
	}

	@Test
	public void testNotWarmedUp() throws Exception {
		doReturn(2).when(requestMonitorPlugin).getNoOfWarmupRequests();
		requestMonitor = new RequestMonitor(configuration, registry);
		final RequestMonitor.RequestInformation requestInformation = requestMonitor.monitor(createMonitoredRequest());
		assertNull(requestInformation.getRequestTrace());
	}

	@Test
	public void testRecordException() throws Exception {
		final MonitoredRequest<RequestTrace> monitoredRequest = createMonitoredRequest();
		doThrow(new RuntimeException("test")).when(monitoredRequest).execute();

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
		doReturn(true).when(corePlugin).isInternalMonitoringActive();

		requestMonitor.monitor(createMonitoredRequest());
		verify(registry, times(0)).timer(name("internal_overhead_request_monitor").build());

		requestMonitor.monitor(createMonitoredRequest());
		verify(registry, times(1)).timer(name("internal_overhead_request_monitor").build());
	}

	private void internalMonitoringTestHelper(boolean active) throws Exception {
		doReturn(active).when(corePlugin).isInternalMonitoringActive();
		requestMonitor.monitor(createMonitoredRequest());
		verify(registry, times(active ? 1 : 0)).timer(name("internal_overhead_request_monitor").build());
	}

	private MonitoredRequest<RequestTrace> createMonitoredRequest() throws Exception {
		@SuppressWarnings("unchecked")
		final MonitoredRequest<RequestTrace> monitoredRequest = mock(MonitoredRequest.class);
		doReturn("test").when(monitoredRequest).execute();
		final RequestTrace requestTrace = new RequestTrace("1");
		requestTrace.setName("test");
		doReturn(requestTrace).when(monitoredRequest).createRequestTrace();

		return monitoredRequest;
	}

	@Test
	public void testProfileThisExecutionDeactive() throws Exception {
		doReturn(0d).when(requestMonitorPlugin).getOnlyCollectNCallTreesPerMinute();
		final RequestMonitor.RequestInformation<RequestTrace> monitor = requestMonitor.monitor(createMonitoredRequest());
		assertNull(monitor.getRequestTrace().getCallStack());
	}

	@Test
	public void testProfileThisExecutionAlwaysActive() throws Exception {
		doReturn(1000000d).when(requestMonitorPlugin).getOnlyCollectNCallTreesPerMinute();
		final RequestMonitor.RequestInformation<RequestTrace> monitor = requestMonitor.monitor(createMonitoredRequest());
		assertNotNull(monitor.getRequestTrace().getCallStack());
	}

	@Test
	public void testDontActivateProfilerWhenNoRequestTraceReporterIsActive() throws Exception {
		// don't profile if no one is interested in the result
		doReturn(0d).when(requestMonitorPlugin).getOnlyReportNRequestsPerMinuteToElasticsearch();
		doReturn(0d).when(requestMonitorPlugin).getOnlyReportNExternalRequestsPerMinute();
		doReturn(1000000d).when(requestMonitorPlugin).getOnlyCollectNCallTreesPerMinute();
		final RequestMonitor.RequestInformation<RequestTrace> monitor = requestMonitor.monitor(createMonitoredRequest());
		assertNull(monitor.getRequestTrace().getCallStack());
	}

	@Test
	public void testProfileThisExecutionActiveEvery2Requests() throws Exception {
		doReturn(2d).when(requestMonitorPlugin).getOnlyCollectNCallTreesPerMinute();
		testProfileThisExecutionHelper(0, true);
		testProfileThisExecutionHelper(1.99, true);
		testProfileThisExecutionHelper(2, false);
		testProfileThisExecutionHelper(3, false);
		testProfileThisExecutionHelper(1, true);
	}

	private void testProfileThisExecutionHelper(double callTreeRate, boolean callStackExpected) throws Exception {
		final Meter callTreeMeter = mock(Meter.class);
		doReturn(callTreeRate).when(callTreeMeter).getOneMinuteRate();
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
		doReturn("testInstance").when(monitoredRequest).getInstanceName();
		requestMonitor.monitor(monitoredRequest);
		assertEquals("testInstance", Stagemonitor.getMeasurementSession().getInstanceName());
	}
}
