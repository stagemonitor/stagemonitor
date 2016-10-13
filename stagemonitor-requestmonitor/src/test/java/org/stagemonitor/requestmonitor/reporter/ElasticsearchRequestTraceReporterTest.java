package org.stagemonitor.requestmonitor.reporter;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestTrace;

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

public class ElasticsearchRequestTraceReporterTest extends AbstractElasticsearchRequestTraceReporterTest {

	private ElasticsearchRequestTraceReporter reporter;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		reporter = new ElasticsearchRequestTraceReporter(requestTraceLogger);
		reporter.init(new SpanReporter.InitArguments(configuration));
	}

	@Test
	public void testReportRequestTrace() throws Exception {
		final RequestTrace requestTrace = mock(RequestTrace.class);
		when(requestTrace.getName()).thenReturn("Report Me");

		reporter.report(new SpanReporter.ReportArguments(requestTrace, null));

		verify(elasticsearchClient).index(anyString(), anyString(), anyObject());
		Assert.assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(requestTrace, null)));
	}

	@Test
	public void testLogReportRequestTrace() throws Exception {
		when(requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()).thenReturn(true);
		final RequestTrace requestTrace = new RequestTrace("abc", new MeasurementSession(getClass().getName(), "test", "test"), requestMonitorPlugin);
		requestTrace.setName("Report Me");

		reporter.report(new SpanReporter.ReportArguments(requestTrace, null));

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), anyObject());
		verify(requestTraceLogger).info(startsWith("{\"index\":{\"_index\":\"stagemonitor-requests-" + StringUtils.getLogstashStyleDate() + "\",\"_type\":\"requests\"}}\n{"));
		Assert.assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(requestTrace, null)));
	}

	@Test
	public void testReportRequestTraceDontReport() throws Exception {
		final RequestTrace requestTrace = mock(RequestTrace.class);
		when(requestTrace.getName()).thenReturn("Regular Foo");

		reporter.report(new SpanReporter.ReportArguments(requestTrace, null));

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), anyObject());
		Assert.assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(requestTrace, null)));
	}

	@Test
	public void testElasticsearchReportingDeactive() throws Exception {
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(0d);
		final RequestTrace requestTrace = mock(RequestTrace.class);
		when(requestTrace.getName()).thenReturn("Report Me");

		assertFalse(reporter.isActive(new SpanReporter.IsActiveArguments(requestTrace, null)));
	}

	@Test
	@Ignore
	public void testElasticsearchReportingRateLimited() throws Exception {
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(1d);
		final RequestTrace requestTrace = mock(RequestTrace.class);
		when(requestTrace.getName()).thenReturn("Report Me");

		assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(requestTrace, null)));
		reporter.report(new SpanReporter.ReportArguments(requestTrace, null));
		Thread.sleep(5010); // the meter only updates every 5 seconds
		assertFalse(reporter.isActive(new SpanReporter.IsActiveArguments(requestTrace, null)));
	}

	@Test
	public void testElasticsearchExcludeCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(1d);

		reporter.report(new SpanReporter.ReportArguments(createTestRequestTraceWithCallTree(1000), null));
		reporter.report(new SpanReporter.ReportArguments(createTestRequestTraceWithCallTree(500), null));
		reporter.report(new SpanReporter.ReportArguments(createTestRequestTraceWithCallTree(250), null));

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

		reporter.report(new SpanReporter.ReportArguments(createTestRequestTraceWithCallTree(250), null));
		reporter.report(new SpanReporter.ReportArguments(createTestRequestTraceWithCallTree(500), null));
		reporter.report(new SpanReporter.ReportArguments(createTestRequestTraceWithCallTree(1000), null));

		ArgumentCaptor<RequestTrace> requestTraceCaptor = ArgumentCaptor.forClass(RequestTrace.class);
		verify(elasticsearchClient, times(3)).index(anyString(), anyString(), requestTraceCaptor.capture());
		RequestTrace requestTrace = requestTraceCaptor.getValue();
		assertTrue((Boolean) requestTrace.getCustomProperties().get("containsCallTree"));
	}

	@Test
	public void testElasticsearchExcludeFastCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(0.85d);

		reporter.report(new SpanReporter.ReportArguments(createTestRequestTraceWithCallTree(1000), null));
		reporter.report(new SpanReporter.ReportArguments(createTestRequestTraceWithCallTree(250), null));

		verify(elasticsearchClient).index(anyString(), anyString(), isA(RequestTrace.class));
		verify(elasticsearchClient).index(anyString(), anyString(), isA(JsonNode.class));
	}

	@Test
	public void testElasticsearchDontExcludeSlowCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(0.85d);

		reporter.report(new SpanReporter.ReportArguments(createTestRequestTraceWithCallTree(250), null));
		reporter.report(new SpanReporter.ReportArguments(createTestRequestTraceWithCallTree(1000), null));

		verify(elasticsearchClient, times(2)).index(anyString(), anyString(), isA(RequestTrace.class));
	}

	@Test
	public void testInterceptorServiceLoader() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(0d);

		reporter.report(new SpanReporter.ReportArguments(createTestRequestTraceWithCallTree(250), null));

		ArgumentCaptor<RequestTrace> requestTraceCaptor = ArgumentCaptor.forClass(RequestTrace.class);
		verify(elasticsearchClient).index(anyString(), anyString(), requestTraceCaptor.capture());
		assertTrue((Boolean) requestTraceCaptor.getValue().getCustomProperties().get("serviceLoaderWorks"));
	}

}
