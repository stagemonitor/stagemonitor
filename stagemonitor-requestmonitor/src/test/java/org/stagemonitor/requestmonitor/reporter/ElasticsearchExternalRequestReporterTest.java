package org.stagemonitor.requestmonitor.reporter;

import com.codahale.metrics.Timer;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.requestmonitor.utils.Spans;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.stagemonitor.requestmonitor.reporter.ExternalRequestMetricsReporter.getExternalRequestTimerName;

public class ElasticsearchExternalRequestReporterTest extends AbstractElasticsearchRequestTraceReporterTest {

	private ElasticsearchSpanReporter reporter;
	private ExternalRequestMetricsReporter externalRequestMetricsReporter;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		when(requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()).thenReturn(true);
		when(requestMonitorPlugin.getOnlyReportNExternalRequestsPerMinute()).thenReturn(1000000d);
		reporter = new ElasticsearchSpanReporter(requestTraceLogger);
		reporter.init(new SpanReporter.InitArguments(configuration));
		externalRequestMetricsReporter = new ExternalRequestMetricsReporter();
		externalRequestMetricsReporter.init(new SpanReporter.InitArguments(configuration));
		when(requestMonitorPlugin.getOnlyReportRequestsWithNameToElasticsearch()).thenReturn(Collections.emptyList());
	}

	@Test
	public void reportRequestTrace() throws Exception {
		when(requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()).thenReturn(false);
		report(getSpan());

		verify(elasticsearchClient).index(startsWith("stagemonitor-spans-"), eq("spans"), any());
		assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(null, getSpan())));
		verifyTimerCreated(1);
	}

	@Test
	public void doNotReportRequestTrace() throws Exception {
		when(requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()).thenReturn(false);
		when(corePlugin.getElasticsearchUrls()).thenReturn(Collections.emptyList());
		when(corePlugin.getElasticsearchUrl()).thenReturn(null);
		report(getSpan());

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), any());
		verify(requestTraceLogger, times(0)).info(anyString());
		assertFalse(reporter.isActive(new SpanReporter.IsActiveArguments(null, getSpan())));
		verifyTimerCreated(1);
	}

	@Test
	public void testLogReportRequestTrace() throws Exception {
		report(getSpan());

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), anyObject());
		verify(requestTraceLogger).info(startsWith("{\"index\":{\"_index\":\"stagemonitor-spans-"));
		assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(null, getSpan())));
		verifyTimerCreated(1);
	}

	@Test
	public void reportRequestTraceRateLimited() throws Exception {
		when(requestMonitorPlugin.getOnlyReportNExternalRequestsPerMinute()).thenReturn(1d);
		report(getSpan());
		verify(requestTraceLogger).info(anyString());
		Thread.sleep(5010); // the meter only updates every 5 seconds
		report(getSpan());
		verifyNoMoreInteractions(requestTraceLogger);
		verifyTimerCreated(2);
	}

	@Test
	public void excludeExternalRequestsFasterThan() throws Exception {
		when(requestMonitorPlugin.getExcludeExternalRequestsFasterThan()).thenReturn(100d);

		report(getSpan(100));
		verify(requestTraceLogger).info(anyString());

		report(getSpan(99));
		verifyNoMoreInteractions(requestTraceLogger);
		verifyTimerCreated(2);
	}

	@Test
	public void testElasticsearchExcludeFastCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeExternalRequestsWhenFasterThanXPercent()).thenReturn(0.85d);

		report(getSpan(1000));
		verify(requestTraceLogger).info(anyString());
		report(getSpan(250));
		verifyNoMoreInteractions(requestTraceLogger);
		verifyTimerCreated(2);
	}

	private void report(Span span) {
		final SpanReporter.IsActiveArguments isActiveArguments = new SpanReporter.IsActiveArguments(null, span);
		final SpanReporter.ReportArguments reportArguments = new SpanReporter.ReportArguments(null, span);
		if (externalRequestMetricsReporter.isActive(isActiveArguments)) {
			externalRequestMetricsReporter.report(reportArguments);
		}
		if (reporter.isActive(isActiveArguments)) {
			reporter.report(reportArguments);
		}
	}

	@Test
	public void testElasticsearchDontExcludeSlowCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeExternalRequestsWhenFasterThanXPercent()).thenReturn(0.85d);

		report(getSpan(250));
		report(getSpan(1000));

		verify(requestTraceLogger, times(2)).info(anyString());
		verifyTimerCreated(2);
	}

	private Span getSpan() {
		return getSpan(1);
	}

	private Span getSpan(long executionTimeMillis) {
		final Tracer tracer = requestMonitorPlugin.getTracer();
		final Span span = tracer.buildSpan("External Request").withStartTimestamp(1).start();
		Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_CLIENT);
		Spans.setOperationType(span, "jdbc");
		span.setTag("method", "SELECT");
		span.finish(TimeUnit.MILLISECONDS.toMicros(executionTimeMillis) + 1);
		return span;
	}

	private void verifyTimerCreated(int count) {
		final Span span = getSpan();
		final Timer timer = registry.getTimers().get(getExternalRequestTimerName((com.uber.jaeger.Span) span));
		assertNotNull(registry.getTimers().keySet().toString(), timer);
		assertEquals(count, timer.getCount());

		final Timer allTimer = registry.getTimers().get(getExternalRequestTimerName((com.uber.jaeger.Span) span, "All"));
		assertNotNull(allTimer);
		assertEquals(count, allTimer.getCount());
	}
}
