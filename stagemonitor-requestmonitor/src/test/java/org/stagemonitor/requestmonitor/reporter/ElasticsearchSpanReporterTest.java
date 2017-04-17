package org.stagemonitor.requestmonitor.reporter;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ElasticsearchSpanReporterTest extends AbstractElasticsearchSpanReporterTest {

	private ElasticsearchSpanReporter reporter;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		reporter = new ElasticsearchSpanReporter(spanLogger);
		reporter.init(configuration);
		JsonUtils.getMapper().registerModule(new ReadbackSpan.SpanJsonModule());
		reportingSpanEventListener.addReporter(reporter);
	}

	@Test
	public void testReportSpan() throws Exception {
		final SpanContextInformation spanContext = SpanContextInformation.forUnitTest(mock(Span.class), "Report Me");
		reporter.report(spanContext);

		verify(elasticsearchClient).index(anyString(), anyString(), any());
		assertTrue(reporter.isActive(spanContext));
	}

	@Test
	public void testLogReportSpan() throws Exception {
		when(requestMonitorPlugin.isOnlyLogElasticsearchSpanReports()).thenReturn(true);
		final SpanContextInformation spanContext = reportSpanWithCallTree(1000, "Report Me");

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), any());
		verify(spanLogger).info(startsWith("{\"index\":{\"_index\":\"stagemonitor-spans-" + StringUtils.getLogstashStyleDate() + "\",\"_type\":\"spans\"}}\n"));
		assertTrue(reporter.isActive(spanContext));
	}

	@Test
	public void testReportSpanDontReport() throws Exception {
		final SpanContextInformation info = reportSpanWithCallTree(1, "Regular Foo");

		assertTrue(reporter.isActive(SpanContextInformation.forUnitTest(info.getSpan())));
		assertFalse(info.isSampled());
		verify(((SpanWrapper) info.getSpan()).getDelegate()).setTag(Tags.SAMPLING_PRIORITY.getKey(), (short) 0);
	}

	@Test
	public void testElasticsearchExcludeCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests()).thenReturn(1d);

		reportSpanWithCallTree(1000, "Report Me");
		reportSpanWithCallTree(500, "Report Me");
		reportSpanWithCallTree(250, "Report Me");

		ArgumentCaptor<ReadbackSpan> spanCaptor = ArgumentCaptor.forClass(ReadbackSpan.class);
		verify(elasticsearchClient, times(3)).index(anyString(), anyString(), spanCaptor.capture());
		ReadbackSpan span = spanCaptor.getValue();
		assertNull(span.getTags().get(SpanUtils.CALL_TREE_ASCII));
		assertNull(span.getTags().get(SpanUtils.CALL_TREE_JSON));
	}

	@Test
	public void testElasticsearchDontExcludeCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests()).thenReturn(0d);

		reportSpanWithCallTree(250, "Report Me");
		reportSpanWithCallTree(500, "Report Me");
		reportSpanWithCallTree(1000, "Report Me");

		ArgumentCaptor<ReadbackSpan> spanCaptor = ArgumentCaptor.forClass(ReadbackSpan.class);
		verify(elasticsearchClient, times(3)).index(anyString(), anyString(), spanCaptor.capture());
		verifyContainsCallTree(spanCaptor.getValue(), true);
	}

	private void verifyContainsCallTree(ReadbackSpan span, boolean contains) {
		assertEquals(contains, span.getTags().get(SpanUtils.CALL_TREE_ASCII) != null);
		assertEquals(contains, span.getTags().get(SpanUtils.CALL_TREE_JSON) != null);
	}

	@Test
	public void testElasticsearchExcludeFastCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests()).thenReturn(0.85d);

		SpanContextInformation spanContext = reportSpanWithCallTree(1000, "Report Me");
		assertFalse(spanContext.getPostExecutionInterceptorContext().isExcludeCallTree());
		verifyContainsCallTree(spanContext.getReadbackSpan(), true);

		spanContext = reportSpanWithCallTree(250, "Report Me");

		assertTrue(spanContext.getPostExecutionInterceptorContext().isExcludeCallTree());
		verifyContainsCallTree(spanContext.getReadbackSpan(), false);
	}

	@Test
	public void testElasticsearchDontExcludeSlowCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests()).thenReturn(0.85d);

		reportSpanWithCallTree(250, "Report Me");
		reportSpanWithCallTree(1000, "Report Me");

		verify(elasticsearchClient, times(2)).index(anyString(), anyString(), isA(ReadbackSpan.class));
	}

	@Test
	public void testInterceptorServiceLoader() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests()).thenReturn(0d);

		reportSpanWithCallTree(250, "Report Me");

		ArgumentCaptor<ReadbackSpan> spanCaptor = ArgumentCaptor.forClass(ReadbackSpan.class);
		verify(elasticsearchClient).index(anyString(), anyString(), spanCaptor.capture());
		ReadbackSpan span = spanCaptor.getValue();
		assertTrue((Boolean) span.getTags().get("serviceLoaderWorks"));
	}
}
