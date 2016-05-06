package org.stagemonitor.requestmonitor.reporter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;

public class ElasticsearchRequestTraceReporterTest {

	private ElasticsearchRequestTraceReporter reporter;
	protected ElasticsearchClient elasticsearchClient;
	protected RequestMonitorPlugin requestMonitorPlugin;
	protected Logger requestTraceLogger;
	protected Metric2Registry registry;
	protected Configuration configuration;

	@Before
	public void setUp() throws Exception {
		configuration = mock(Configuration.class);
		CorePlugin corePlugin = mock(CorePlugin.class);
		requestMonitorPlugin = mock(RequestMonitorPlugin.class);

		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(1000000d);
		when(requestMonitorPlugin.getOnlyReportRequestsWithNameToElasticsearch()).thenReturn(Collections.singleton("Report Me"));
		when(corePlugin.getElasticsearchUrl()).thenReturn("http://localhost:9200");
		when(corePlugin.getElasticsearchUrls()).thenReturn(Collections.singletonList("http://localhost:9200"));
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient = mock(ElasticsearchClient.class));
		registry = new Metric2Registry();
		when(corePlugin.getMetricRegistry()).thenReturn(registry);
		requestTraceLogger = mock(Logger.class);
		reporter = new ElasticsearchRequestTraceReporter(requestTraceLogger);
		reporter.init(new RequestTraceReporter.InitArguments(configuration));
	}

	@Test
	public void testReportRequestTrace() throws Exception {
		final RequestTrace requestTrace = mock(RequestTrace.class);
		when(requestTrace.getName()).thenReturn("Report Me");

		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(requestTrace));

		verify(elasticsearchClient).index(anyString(), anyString(), anyObject());
		Assert.assertTrue(reporter.isActive(new RequestTraceReporter.IsActiveArguments(requestTrace)));
	}

	@Test
	public void testLogReportRequestTrace() throws Exception {
		when(requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()).thenReturn(true);
		final RequestTrace requestTrace = new RequestTrace("abc", new MeasurementSession(getClass().getName(), "test", "test"), requestMonitorPlugin);
		requestTrace.setName("Report Me");

		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(requestTrace));

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), anyObject());
		verify(requestTraceLogger).info(startsWith("{\"index\":{\"_index\":\"stagemonitor-requests-" + StringUtils.getLogstashStyleDate() + "\",\"_type\":\"requests\"}}\n{"));
		Assert.assertTrue(reporter.isActive(new RequestTraceReporter.IsActiveArguments(requestTrace)));
	}

	@Test
	public void testReportRequestTraceDontReport() throws Exception {
		final RequestTrace requestTrace = mock(RequestTrace.class);
		when(requestTrace.getName()).thenReturn("Regular Foo");

		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(requestTrace));

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), anyObject());
		Assert.assertTrue(reporter.isActive(new RequestTraceReporter.IsActiveArguments(requestTrace)));
	}

	@Test
	public void testElasticsearchReportingDeactive() throws Exception {
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(0d);
		final RequestTrace requestTrace = mock(RequestTrace.class);
		when(requestTrace.getName()).thenReturn("Report Me");

		assertFalse(reporter.isActive(new RequestTraceReporter.IsActiveArguments(requestTrace)));
	}

	@Test
	@Ignore
	public void testElasticsearchReportingRateLimited() throws Exception {
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(1d);
		final RequestTrace requestTrace = mock(RequestTrace.class);
		when(requestTrace.getName()).thenReturn("Report Me");

		assertTrue(reporter.isActive(new RequestTraceReporter.IsActiveArguments(requestTrace)));
		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(requestTrace));
		Thread.sleep(5010); // the meter only updates every 5 seconds
		assertFalse(reporter.isActive(new RequestTraceReporter.IsActiveArguments(requestTrace)));
	}

	@Test
	public void testElasticsearchExcludeCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(1d);

		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(createTestRequestTraceWithCallTree(1000)));
		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(createTestRequestTraceWithCallTree(500)));
		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(createTestRequestTraceWithCallTree(250)));

		ArgumentCaptor<JsonNode> requestTraceCaptor = ArgumentCaptor.forClass(JsonNode.class);
		verify(elasticsearchClient, times(3)).index(anyString(), anyString(), requestTraceCaptor.capture());
		JsonNode requestTrace = requestTraceCaptor.getValue();
		assertFalse(requestTrace.has("callStack"));
		assertFalse(requestTrace.has("callStackJson"));
		assertFalse(requestTrace.get("containsCallTree").booleanValue());
	}

	@Test
	public void testElasticsearchDontExcludeCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(0d);

		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(createTestRequestTraceWithCallTree(250)));
		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(createTestRequestTraceWithCallTree(500)));
		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(createTestRequestTraceWithCallTree(1000)));

		ArgumentCaptor<RequestTrace> requestTraceCaptor = ArgumentCaptor.forClass(RequestTrace.class);
		verify(elasticsearchClient, times(3)).index(anyString(), anyString(), requestTraceCaptor.capture());
		RequestTrace requestTrace = requestTraceCaptor.getValue();
		assertTrue((Boolean) requestTrace.getCustomProperties().get("containsCallTree"));
	}

	@Test
	public void testElasticsearchExcludeFastCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(0.85d);

		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(createTestRequestTraceWithCallTree(1000)));
		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(createTestRequestTraceWithCallTree(250)));

		verify(elasticsearchClient).index(anyString(), anyString(), isA(RequestTrace.class));
		verify(elasticsearchClient).index(anyString(), anyString(), isA(JsonNode.class));
	}

	@Test
	public void testElasticsearchDontExcludeSlowCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(0.85d);

		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(createTestRequestTraceWithCallTree(250)));
		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(createTestRequestTraceWithCallTree(1000)));

		verify(elasticsearchClient, times(2)).index(anyString(), anyString(), isA(RequestTrace.class));
	}

	@Test
	public void testInterceptorServiceLoader() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(0d);

		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(createTestRequestTraceWithCallTree(250)));

		ArgumentCaptor<RequestTrace> requestTraceCaptor = ArgumentCaptor.forClass(RequestTrace.class);
		verify(elasticsearchClient).index(anyString(), anyString(), requestTraceCaptor.capture());
		assertTrue((Boolean) requestTraceCaptor.getValue().getCustomProperties().get("serviceLoaderWorks"));
	}

	private RequestTrace createTestRequestTraceWithCallTree(long executionTime) {
		final RequestTrace requestTrace = new RequestTrace(UUID.randomUUID().toString(), new MeasurementSession("ERTRT", "test", "test"), requestMonitorPlugin);
		requestTrace.setCallStack(CallStackElement.createRoot("test"));
		requestTrace.setName("Report Me");
		requestTrace.setExecutionTime(executionTime);
		registry.timer(RequestMonitor.getTimerMetricName(requestTrace.getName())).update(executionTime, TimeUnit.NANOSECONDS);
		return requestTrace;
	}
}