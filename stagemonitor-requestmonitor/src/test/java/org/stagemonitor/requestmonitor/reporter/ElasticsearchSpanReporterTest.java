package org.stagemonitor.requestmonitor.reporter;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.tracing.jaeger.SpanJsonModule;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
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
		JsonUtils.getMapper().registerModule(new SpanJsonModule());
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
		final SpanContextInformation spanContext = createTestSpanWithCallTree(1000, "Report Me");

		reporter.report(spanContext);

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), any());
		verify(spanLogger).info(startsWith("{\"index\":{\"_index\":\"stagemonitor-spans-" + StringUtils.getLogstashStyleDate() + "\",\"_type\":\"spans\"}}\n"));
		assertTrue(reporter.isActive(spanContext));
	}

	@Test
	public void testReportSpanDontReport() throws Exception {
		final SpanContextInformation info = createTestSpanWithCallTree(1, "Regular Foo");

		assertTrue(reporter.isActive(SpanContextInformation.forUnitTest(info.getSpan())));
		assertFalse(info.isSampled());
		verify(((SpanWrapper) info.getSpan()).getDelegate()).setTag(Tags.SAMPLING_PRIORITY.getKey(), (short) 0);
	}

	@Test
	@Ignore
	public void testElasticsearchReportingRateLimited() throws Exception {
		when(requestMonitorPlugin.getRateLimitServerSpansPerMinute()).thenReturn(1d);
		final Span span = mock(Span.class);

		assertTrue(reporter.isActive(SpanContextInformation.forUnitTest(span)));
		reporter.report(SpanContextInformation.forUnitTest(span, "Regular Foo"));
		Thread.sleep(5010); // the meter only updates every 5 seconds
		assertFalse(reporter.isActive(SpanContextInformation.forUnitTest(span)));
	}

	@Test
	public void testElasticsearchExcludeCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests()).thenReturn(1d);

		reporter.report(createTestSpanWithCallTree(1000, "Report Me"));
		reporter.report(createTestSpanWithCallTree(500, "Report Me"));
		reporter.report(createTestSpanWithCallTree(250, "Report Me"));

		ArgumentCaptor<SpanWrapper> spanCaptor = ArgumentCaptor.forClass(SpanWrapper.class);
		verify(elasticsearchClient, times(3)).index(anyString(), anyString(), spanCaptor.capture());
		SpanWrapper span = spanCaptor.getValue();
		verify(span.getDelegate(), times(0)).setTag(eq(SpanUtils.CALL_TREE_ASCII), anyString());
		verify(span.getDelegate(), times(0)).setTag(eq(SpanUtils.CALL_TREE_JSON), anyString());
	}

	@Test
	public void testElasticsearchDontExcludeCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests()).thenReturn(0d);

		reporter.report(createTestSpanWithCallTree(250, "Report Me"));
		reporter.report(createTestSpanWithCallTree(500, "Report Me"));
		reporter.report(createTestSpanWithCallTree(1000, "Report Me"));

		ArgumentCaptor<SpanWrapper> spanCaptor = ArgumentCaptor.forClass(SpanWrapper.class);
		verify(elasticsearchClient, times(3)).index(anyString(), anyString(), spanCaptor.capture());
		verifyContainsCallTree(spanCaptor.getValue(), true);
	}

	private void verifyContainsCallTree(SpanWrapper span, boolean contains) {
		verify(span.getDelegate(), times(contains ? 1 : 0)).setTag(eq(SpanUtils.CALL_TREE_ASCII), anyString());
		verify(span.getDelegate(), times(contains ? 1 : 0)).setTag(eq(SpanUtils.CALL_TREE_JSON), anyString());
	}

	@Test
	public void testElasticsearchExcludeFastCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests()).thenReturn(0.85d);

		SpanContextInformation spanContext = createTestSpanWithCallTree(1000, "Report Me");
		reporter.report(spanContext);
		assertFalse(spanContext.getPostExecutionInterceptorContext().isExcludeCallTree());
		verifyContainsCallTree((SpanWrapper) spanContext.getSpan(), true);

		spanContext = createTestSpanWithCallTree(250, "Report Me");
		reporter.report(spanContext);

		assertTrue(spanContext.getPostExecutionInterceptorContext().isExcludeCallTree());
		verifyContainsCallTree((SpanWrapper) spanContext.getSpan(), false);
	}

	@Test
	public void testElasticsearchDontExcludeSlowCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests()).thenReturn(0.85d);

		reporter.report(createTestSpanWithCallTree(250, "Report Me"));
		reporter.report(createTestSpanWithCallTree(1000, "Report Me"));

		verify(elasticsearchClient, times(2)).index(anyString(), anyString(), isA(Span.class));
	}

	@Test
	public void testInterceptorServiceLoader() throws Exception {
		when(requestMonitorPlugin.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests()).thenReturn(0d);

		reporter.report(createTestSpanWithCallTree(250, "Report Me"));

		ArgumentCaptor<SpanWrapper> spanCaptor = ArgumentCaptor.forClass(SpanWrapper.class);
		verify(elasticsearchClient).index(anyString(), anyString(), spanCaptor.capture());
		SpanWrapper span = spanCaptor.getValue();
		verify(span.getDelegate()).setTag("serviceLoaderWorks", true);
	}


}
