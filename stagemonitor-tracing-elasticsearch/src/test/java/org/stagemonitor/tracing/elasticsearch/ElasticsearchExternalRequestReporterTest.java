package org.stagemonitor.tracing.elasticsearch;

import com.codahale.metrics.Timer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.stagemonitor.tracing.NoopSpan;
import org.stagemonitor.tracing.RequestMonitor;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.metrics.ExternalRequestMetricsSpanEventListener;
import org.stagemonitor.tracing.reporter.ReadbackSpan;
import org.stagemonitor.tracing.reporter.ReadbackSpanEventListener;
import org.stagemonitor.tracing.reporter.ReportingSpanEventListener;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class ElasticsearchExternalRequestReporterTest extends AbstractElasticsearchSpanReporterTest {

	private ElasticsearchSpanReporter reporter;
	private ReportingSpanEventListener reportingSpanEventListener;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		when(tracingPlugin.getDefaultRateLimitSpansPerMinute()).thenReturn(1000000d);
		reporter = new ElasticsearchSpanReporter(spanLogger);
		reporter.init(configuration);
		when(tracingPlugin.getOnlyReportSpansWithName()).thenReturn(Collections.emptyList());
		final RequestMonitor requestMonitor = mock(RequestMonitor.class);
		when(tracingPlugin.getRequestMonitor()).thenReturn(requestMonitor);
		reportingSpanEventListener = mock(ReportingSpanEventListener.class);
	}

	@Test
	public void reportSpan() throws Exception {
		when(elasticsearchTracingPlugin.isOnlyLogElasticsearchSpanReports()).thenReturn(false);
		final ReadbackSpan span = getSpan();
		report(span);

		Mockito.verify(elasticsearchClient).index(ArgumentMatchers.startsWith("stagemonitor-spans-"), ArgumentMatchers.eq("spans"), ArgumentMatchers.any());
		Assert.assertTrue(reporter.isActive(null));
		verifyTimerCreated(1);
	}

	@Test
	public void doNotReportSpan() throws Exception {
		when(elasticsearchTracingPlugin.isOnlyLogElasticsearchSpanReports()).thenReturn(false);
		when(elasticsearchClient.isElasticsearchAvailable()).thenReturn(false);
		when(corePlugin.getElasticsearchUrl()).thenReturn(null);
		final ReadbackSpan span = getSpan();
		report(span);

		Mockito.verify(elasticsearchClient, Mockito.times(0)).index(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any());
		Mockito.verify(spanLogger, Mockito.times(0)).info(ArgumentMatchers.anyString());
		Assert.assertFalse(reporter.isActive(null));
		verifyTimerCreated(1);
	}

	@Test
	public void testLogReportSpan() throws Exception {
		when(elasticsearchTracingPlugin.isOnlyLogElasticsearchSpanReports()).thenReturn(true);

		report(getSpan());
		Mockito.verify(elasticsearchClient, Mockito.times(0)).index(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any());
		Mockito.verify(spanLogger).info(ArgumentMatchers.startsWith("{\"index\":{\"_index\":\"stagemonitor-spans-"));
	}

	@Test
	public void reportSpanRateLimited() throws Exception {
		when(tracingPlugin.getDefaultRateLimitSpansPerMinute()).thenReturn(1d);
		report(getSpan());
		Mockito.verify(elasticsearchClient).index(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any());
		Thread.sleep(5010); // the meter only updates every 5 seconds
		report(getSpan());
		Mockito.verifyNoMoreInteractions(spanLogger);
		verifyTimerCreated(2);
	}

	@Test
	public void excludeExternalRequestsFasterThan() throws Exception {
		when(tracingPlugin.getExcludeExternalRequestsFasterThan()).thenReturn(100d);

		report(getSpan(100));
		Mockito.verify(elasticsearchClient).index(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any());

		report(getSpan(99));
		Mockito.verifyNoMoreInteractions(spanLogger);
		verifyTimerCreated(2);
	}

	@Test
	public void testElasticsearchExcludeFastCallTree() throws Exception {
		when(tracingPlugin.getExcludeExternalRequestsWhenFasterThanXPercent()).thenReturn(0.85d);

		report(getSpan(1000));
		Mockito.verify(elasticsearchClient).index(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any());
		report(getSpan(250));
		Mockito.verifyNoMoreInteractions(spanLogger);
		verifyTimerCreated(2);
	}

	private void report(ReadbackSpan span) {
		if (reporter.isActive(null)) {
			reporter.report(mock(SpanContextInformation.class), span);
		}
	}

	@Test
	public void testElasticsearchDontExcludeSlowCallTree() throws Exception {
		when(tracingPlugin.getExcludeExternalRequestsWhenFasterThanXPercent()).thenReturn(0.85d);

		report(getSpan(250));
		report(getSpan(1000));

		Mockito.verify(elasticsearchClient, Mockito.times(2)).index(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any());
		verifyTimerCreated(2);
	}

	private ReadbackSpan getSpan() {
		return getSpan(1);
	}

	private ReadbackSpan getSpan(long executionTimeMillis) {
		final ReadbackSpanEventListener readbackSpanEventListener = new ReadbackSpanEventListener(reportingSpanEventListener, tracingPlugin);
		final Span span = new SpanWrapper(NoopSpan.INSTANCE, "External Request", 1,
				1, Arrays.asList(new ExternalRequestMetricsSpanEventListener(registry), readbackSpanEventListener));
		Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_CLIENT);
		span.setTag(SpanUtils.OPERATION_TYPE, "jdbc");
		span.setTag("method", "SELECT");
		span.finish(TimeUnit.MILLISECONDS.toMicros(executionTimeMillis) + 1);
		return readbackSpanEventListener.getReadbackSpan();
	}

	private void verifyTimerCreated(int count) {
		final Timer timer = registry.getTimers().get(name("external_request_response_time")
				.tag("type", "jdbc")
				.tag("signature", "External Request")
				.tag("method", "SELECT")
				.build());
		Assert.assertNotNull(registry.getTimers().keySet().toString(), timer);
		Assert.assertEquals(count, timer.getCount());

		final Timer allTimer = registry.getTimers().get(name("external_request_response_time")
				.tag("type", "jdbc")
				.tag("signature", "All")
				.tag("method", "SELECT")
				.build());
		Assert.assertNotNull(allTimer);
		Assert.assertEquals(count, allTimer.getCount());
	}
}
