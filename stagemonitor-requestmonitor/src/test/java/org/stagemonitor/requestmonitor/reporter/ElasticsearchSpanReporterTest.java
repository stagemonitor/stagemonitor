package org.stagemonitor.requestmonitor.reporter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uber.jaeger.Span;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.utils.SpanTags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
	}

	@Test
	public void testReportRequestTrace() throws Exception {
		final Span span = mock(Span.class);
		when(span.getOperationName()).thenReturn("Report Me");

		reporter.report(new SpanReporter.ReportArguments(span, null));

		verify(elasticsearchClient).index(anyString(), anyString(), any());
		Assert.assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(span)));
	}

	@Test
	public void testLogReportRequestTrace() throws Exception {
		when(requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()).thenReturn(true);
		final Span span = createTestSpanWithCallTree(1000);

		reporter.report(new SpanReporter.ReportArguments(span, null));

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), any());
		verify(requestTraceLogger).info(startsWith("{\"index\":{\"_index\":\"stagemonitor-spans-" + StringUtils.getLogstashStyleDate() + "\",\"_type\":\"spans\"}}\n{"));
		Assert.assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(span)));
	}

	@Test
	public void testReportRequestTraceDontReport() throws Exception {
		final Span span = mock(Span.class);
		when(span.getOperationName()).thenReturn("Regular Foo");

		reporter.report(new SpanReporter.ReportArguments(span, null));

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), any());
		Assert.assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(span)));
	}

	@Test
	public void testElasticsearchReportingDeactive() throws Exception {
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(0d);
		final Span span = mock(Span.class);
		when(span.getOperationName()).thenReturn("Report Me");

		assertFalse(reporter.isActive(new SpanReporter.IsActiveArguments(span)));
	}

	@Test
	@Ignore
	public void testElasticsearchReportingRateLimited() throws Exception {
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(1d);
		final Span span = mock(Span.class);
		when(span.getOperationName()).thenReturn("Report Me");

		assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(span)));
		reporter.report(new SpanReporter.ReportArguments(span, null));
		Thread.sleep(5010); // the meter only updates every 5 seconds
		assertFalse(reporter.isActive(new SpanReporter.IsActiveArguments(span)));
	}

	@Test
	public void testElasticsearchExcludeCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(1d);

		reporter.report(new SpanReporter.ReportArguments(createTestSpanWithCallTree(1000), null));
		reporter.report(new SpanReporter.ReportArguments(createTestSpanWithCallTree(500), null));
		reporter.report(new SpanReporter.ReportArguments(createTestSpanWithCallTree(250), null));

		ArgumentCaptor<JsonNode> requestTraceCaptor = ArgumentCaptor.forClass(JsonNode.class);
		verify(elasticsearchClient, times(3)).index(anyString(), anyString(), requestTraceCaptor.capture());
		JsonNode span = requestTraceCaptor.getValue();
		assertFalse(span.has(SpanTags.CALL_TREE_ASCII));
		assertFalse(span.has(SpanTags.CALL_TREE_JSON));
		assertFalse(span.get("contains_call_tree").booleanValue());
	}

	@Test
	public void testElasticsearchDontExcludeCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(0d);

		reporter.report(new SpanReporter.ReportArguments(createTestSpanWithCallTree(250), null));
		reporter.report(new SpanReporter.ReportArguments(createTestSpanWithCallTree(500), null));
		reporter.report(new SpanReporter.ReportArguments(createTestSpanWithCallTree(1000), null));

		ArgumentCaptor<Span> requestTraceCaptor = ArgumentCaptor.forClass(Span.class);
		verify(elasticsearchClient, times(3)).index(anyString(), anyString(), requestTraceCaptor.capture());
		Span span = requestTraceCaptor.getValue();
		assertTrue((Boolean) span.getTags().get("contains_call_tree"));
	}

	@Test
	public void testElasticsearchExcludeFastCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(0.85d);

		reporter.report(new SpanReporter.ReportArguments(createTestSpanWithCallTree(1000), null));
		reporter.report(new SpanReporter.ReportArguments(createTestSpanWithCallTree(250), null));

		verify(elasticsearchClient).index(anyString(), anyString(), isA(Span.class));
		verify(elasticsearchClient).index(anyString(), anyString(), isA(JsonNode.class));
	}

	@Test
	public void testElasticsearchDontExcludeSlowCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(0.85d);

		reporter.report(new SpanReporter.ReportArguments(createTestSpanWithCallTree(250), null));
		reporter.report(new SpanReporter.ReportArguments(createTestSpanWithCallTree(1000), null));

		verify(elasticsearchClient, times(2)).index(anyString(), anyString(), isA(Span.class));
	}

	@Test
	public void testInterceptorServiceLoader() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests()).thenReturn(0d);

		reporter.report(new SpanReporter.ReportArguments(createTestSpanWithCallTree(250), null));

		ArgumentCaptor<Span> requestTraceCaptor = ArgumentCaptor.forClass(Span.class);
		verify(elasticsearchClient).index(anyString(), anyString(), requestTraceCaptor.capture());
		assertTrue((Boolean) requestTraceCaptor.getValue().getTags().get("serviceLoaderWorks"));
	}

	@Test
	public void testNestDottedTagKeys() {
		final io.opentracing.Span span = createTestSpan(1);
		span.setTag("a.b.c.d1", "1");
		span.setTag("a.b.c.d2", "2");
		final ObjectNode jsonSpan = JsonUtils.toObjectNode(span);
		System.out.println(jsonSpan);
		assertEquals("1", jsonSpan.get("a").get("b").get("c").get("d1").asText());
		assertEquals("2", jsonSpan.get("a").get("b").get("c").get("d2").asText());
	}

	@Test
	public void testSampledTag() {
		final io.opentracing.Span span = createTestSpan(1);
		span.setTag("duration", "foo");
		final ObjectNode jsonSpan = JsonUtils.toObjectNode(span);
		System.out.println(jsonSpan);
		assertEquals(1000, jsonSpan.get("duration").intValue());
	}

	@Test
	public void testAmbiguousMapping() {
		final io.opentracing.Span span = createTestSpan(1);
		span.setTag("a", "1");
		span.setTag("a.b", "2");
		try {
			System.out.println(JsonUtils.toObjectNode(span));
			fail();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			assertTrue(e.getMessage().startsWith("Ambiguous mapping for"));
		}
	}

}
