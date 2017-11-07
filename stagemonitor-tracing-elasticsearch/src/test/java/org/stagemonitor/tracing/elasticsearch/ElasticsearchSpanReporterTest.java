package org.stagemonitor.tracing.elasticsearch;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.reporter.SpanReporter;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.opentracing.mock.MockSpan;
import io.opentracing.tag.Tags;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

public class ElasticsearchSpanReporterTest extends AbstractElasticsearchSpanReporterTest {

	private ElasticsearchSpanReporter reporter;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		reporter = new ElasticsearchSpanReporter(spanLogger);
		reporter.init(configuration);
		reportingSpanEventListener.addReporter(reporter);
	}

	@Test
	public void testReportSpan() throws Exception {
		final SpanContextInformation spanContext = reportSpanWithCallTree(1000, "Report Me");

		final List<MockSpan> sampledSpans = getSampledSpans();
		assertThat(sampledSpans).hasSize(1);
		Mockito.verify(httpClient).send(any(), any(), any(), any(), any());
		Assert.assertTrue(reporter.isActive(spanContext));
	}

	@Test
	public void testLogReportSpan() throws Exception {
		Mockito.when(elasticsearchTracingPlugin.isOnlyLogElasticsearchSpanReports()).thenReturn(true);
		final SpanContextInformation spanContext = reportSpanWithCallTree(1000, "Report Me");

		final List<MockSpan> sampledSpans = getSampledSpans();
		assertThat(sampledSpans).hasSize(1);
		Mockito.verify(httpClient, Mockito.times(0)).send(any(), any(), any(), any(), any());
		Mockito.verify(spanLogger).info(ArgumentMatchers.startsWith("{\"index\":{\"_index\":\"stagemonitor-spans-" + StringUtils.getLogstashStyleDate() + "\",\"_type\":\"spans\"}}\n"));
		Assert.assertTrue(reporter.isActive(spanContext));
	}

	@Test
	public void testReportSpanDontReport() throws Exception {
		final SpanContextInformation info = reportSpanWithCallTree(1, "Regular Foo");

		Assert.assertTrue(reporter.isActive(info));
		assertEquals(0, tags.get(Tags.SAMPLING_PRIORITY.getKey()));
	}

	@Test
	public void testElasticsearchExcludeCallTree() throws Exception {
		Mockito.when(tracingPlugin.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests()).thenReturn(1d);

		reportSpanWithCallTree(1000, "Report Me");
		reportSpanWithCallTree(500, "Report Me");
		reportSpanWithCallTree(250, "Report Me");

		final List<MockSpan> sampledSpans = getSampledSpans();
		assertThat(sampledSpans).hasSize(3);
		sampledSpans.forEach(span -> assertThat(span.tags()).doesNotContainKeys(SpanUtils.CALL_TREE_ASCII, SpanUtils.CALL_TREE_JSON));
	}

	private List<MockSpan> getSampledSpans() {
		return mockTracer.finishedSpans()
				.stream()
				.filter(span -> !Integer.valueOf(0).equals(span.tags().get(Tags.SAMPLING_PRIORITY.getKey())))
				.collect(Collectors.toList());
	}

	@Test
	public void testElasticsearchDontExcludeCallTree() throws Exception {
		Mockito.when(tracingPlugin.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests()).thenReturn(0d);

		reportSpanWithCallTree(250, "Report Me");
		reportSpanWithCallTree(500, "Report Me");
		reportSpanWithCallTree(1000, "Report Me");

		final List<MockSpan> sampledSpans = getSampledSpans();
		assertThat(sampledSpans).hasSize(3);
		sampledSpans.forEach(span -> assertThat(span.tags()).containsKeys(SpanUtils.CALL_TREE_ASCII, SpanUtils.CALL_TREE_JSON));
	}

	@Test
	public void testElasticsearchExcludeFastCallTree() throws Exception {
		Mockito.when(tracingPlugin.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests()).thenReturn(0.85d);

		reportSpanWithCallTree(1000, "Report Me");
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		assertThat(mockTracer.finishedSpans().get(0).tags()).containsKeys(SpanUtils.CALL_TREE_ASCII, SpanUtils.CALL_TREE_JSON);

		mockTracer.reset();

		reportSpanWithCallTree(250, "Report Me");
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		assertThat(mockTracer.finishedSpans().get(0).tags()).doesNotContainKeys(SpanUtils.CALL_TREE_ASCII, SpanUtils.CALL_TREE_JSON);
	}

	@Test
	public void testElasticsearchDontExcludeSlowCallTree() throws Exception {
		Mockito.when(tracingPlugin.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests()).thenReturn(0.85d);

		reportSpanWithCallTree(250, "Report Me");
		reportSpanWithCallTree(1000, "Report Me");

		assertThat(getSampledSpans()).hasSize(2);
	}

	@Test
	public void testInterceptorServiceLoader() throws Exception {
		Mockito.when(tracingPlugin.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests()).thenReturn(0d);

		reportSpanWithCallTree(250, "Report Me");

		final List<MockSpan> sampledSpans = getSampledSpans();
		assertThat(sampledSpans).hasSize(1);
		assertThat(sampledSpans.get(0).tags()).containsEntry("serviceLoaderWorks", true);
	}

	@Test
	public void testLoadedViaServiceLoader() throws Exception {

		assertThat(StreamSupport.stream(ServiceLoader.load(SpanReporter.class).spliterator(), false)
				.filter(reporter -> reporter instanceof ElasticsearchSpanReporter)).hasSize(1);
	}

	@Test
	public void testToBulkUpdateBytes() throws Exception {
		final ElasticsearchUpdateSpanReporter.BulkUpdateOutputStreamHandler bulkUpdateOutputStreamHandler =
				new ElasticsearchUpdateSpanReporter.BulkUpdateOutputStreamHandler("test-id", Collections.singletonMap("foo", "bar"));
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		bulkUpdateOutputStreamHandler.withHttpURLConnection(output);
		assertThat(output.toString())
				.isEqualTo("{\"update\":{\"_id\":\"test-id\"}}\n" +
						"{\"doc\":{\"foo\":\"bar\"}}\n");
	}
}
