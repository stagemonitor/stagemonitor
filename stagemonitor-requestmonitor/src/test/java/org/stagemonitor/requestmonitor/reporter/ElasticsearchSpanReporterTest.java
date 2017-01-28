package org.stagemonitor.requestmonitor.reporter;

import com.fasterxml.jackson.databind.JsonNode;
import com.uber.jaeger.Span;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.tracing.SpanJsonModule;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ElasticsearchSpanReporterTest extends AbstractElasticsearchRequestTraceReporterTest {

	private ElasticsearchSpanReporter reporter;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		reporter = new ElasticsearchSpanReporter(requestTraceLogger);
		reporter.init(new SpanReporter.InitArguments(configuration, registry));
		JsonUtils.getMapper().registerModule(new SpanJsonModule());
	}

	@Test
	public void testReportRequestTrace() throws Exception {
		final Span span = mock(Span.class);
		when(span.getOperationName()).thenReturn("Report Me");

		reporter.report(RequestMonitor.RequestInformation.of(span, null, Collections.<String, Object>emptyMap()));

		verify(elasticsearchClient).index(anyString(), anyString(), any());
		Assert.assertTrue(reporter.isActive(RequestMonitor.RequestInformation.of(span, null)));
	}

	@Test
	public void testLogReportRequestTrace() throws Exception {
		when(requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()).thenReturn(true);
		final Span span = createTestSpanWithCallTree(1000);

		reporter.report(RequestMonitor.RequestInformation.of(span, null, Collections.<String, Object>emptyMap()));

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), any());
		verify(requestTraceLogger).info(startsWith("{\"index\":{\"_index\":\"stagemonitor-spans-" + StringUtils.getLogstashStyleDate() + "\",\"_type\":\"spans\"}}\n{"));
		Assert.assertTrue(reporter.isActive(RequestMonitor.RequestInformation.of(span, null)));
	}

	@Test
	public void testReportRequestTraceDontReport() throws Exception {
		final Span span = mock(Span.class);
		when(span.getOperationName()).thenReturn("Regular Foo");

		reporter.report(RequestMonitor.RequestInformation.of(span, null, Collections.<String, Object>emptyMap()));

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), any());
		Assert.assertTrue(reporter.isActive(RequestMonitor.RequestInformation.of(span, null)));
	}

	@Test
	public void testElasticsearchReportingDeactive() throws Exception {
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(0d);
		final Span span = mock(Span.class);
		when(span.getOperationName()).thenReturn("Report Me");

		assertFalse(reporter.isActive(RequestMonitor.RequestInformation.of(span, null)));
	}

	@Test
	@Ignore
	public void testElasticsearchReportingRateLimited() throws Exception {
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(1d);
		final Span span = mock(Span.class);
		when(span.getOperationName()).thenReturn("Report Me");

		assertTrue(reporter.isActive(RequestMonitor.RequestInformation.of(span, null)));
		reporter.report(RequestMonitor.RequestInformation.of(span, null, Collections.<String, Object>emptyMap()));
		Thread.sleep(5010); // the meter only updates every 5 seconds
		assertFalse(reporter.isActive(RequestMonitor.RequestInformation.of(span, null)));
	}

	@Test
	public void testElasticsearchExcludeCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(1d);

		reporter.report(RequestMonitor.RequestInformation.of(createTestSpanWithCallTree(1000), null, Collections.<String, Object>emptyMap()));
		reporter.report(RequestMonitor.RequestInformation.of(createTestSpanWithCallTree(500), null, Collections.<String, Object>emptyMap()));
		reporter.report(RequestMonitor.RequestInformation.of(createTestSpanWithCallTree(250), null, Collections.<String, Object>emptyMap()));

		ArgumentCaptor<JsonNode> requestTraceCaptor = ArgumentCaptor.forClass(JsonNode.class);
		verify(elasticsearchClient, times(3)).index(anyString(), anyString(), requestTraceCaptor.capture());
		JsonNode span = requestTraceCaptor.getValue();
		assertFalse(span.has(SpanUtils.CALL_TREE_ASCII));
		assertFalse(span.has(SpanUtils.CALL_TREE_JSON));
	}

	@Test
	public void testElasticsearchDontExcludeCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(0d);

		reporter.report(RequestMonitor.RequestInformation.of(createTestSpanWithCallTree(250), null, Collections.<String, Object>emptyMap()));
		reporter.report(RequestMonitor.RequestInformation.of(createTestSpanWithCallTree(500), null, Collections.<String, Object>emptyMap()));
		reporter.report(RequestMonitor.RequestInformation.of(createTestSpanWithCallTree(1000), null, Collections.<String, Object>emptyMap()));

		ArgumentCaptor<Span> requestTraceCaptor = ArgumentCaptor.forClass(Span.class);
		verify(elasticsearchClient, times(3)).index(anyString(), anyString(), requestTraceCaptor.capture());
	}

	@Test
	public void testElasticsearchExcludeFastCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(0.85d);

		reporter.report(RequestMonitor.RequestInformation.of(createTestSpanWithCallTree(1000), null, Collections.<String, Object>emptyMap()));
		reporter.report(RequestMonitor.RequestInformation.of(createTestSpanWithCallTree(250), null, Collections.<String, Object>emptyMap()));

		verify(elasticsearchClient).index(anyString(), anyString(), isA(Span.class));
		verify(elasticsearchClient).index(anyString(), anyString(), isA(JsonNode.class));
	}

	@Test
	public void testElasticsearchDontExcludeSlowCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(0.85d);

		reporter.report(RequestMonitor.RequestInformation.of(createTestSpanWithCallTree(250), null, Collections.<String, Object>emptyMap()));
		reporter.report(RequestMonitor.RequestInformation.of(createTestSpanWithCallTree(1000), null, Collections.<String, Object>emptyMap()));

		verify(elasticsearchClient, times(2)).index(anyString(), anyString(), isA(Span.class));
	}

	@Test
	public void testInterceptorServiceLoader() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(0d);

		reporter.report(RequestMonitor.RequestInformation.of(createTestSpanWithCallTree(250), null, Collections.<String, Object>emptyMap()));

		ArgumentCaptor<Span> requestTraceCaptor = ArgumentCaptor.forClass(Span.class);
		verify(elasticsearchClient).index(anyString(), anyString(), requestTraceCaptor.capture());
		assertTrue((Boolean) requestTraceCaptor.getValue().getTags().get("serviceLoaderWorks"));
	}


}
