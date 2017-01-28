package org.stagemonitor.requestmonitor.reporter;

import com.codahale.metrics.Timer;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.metrics.ExternalRequestMetricsSpanInterceptor;
import org.stagemonitor.requestmonitor.tracing.SpanJsonModule;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class ElasticsearchExternalRequestReporterTest extends AbstractElasticsearchRequestTraceReporterTest {

	private ElasticsearchSpanReporter reporter;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		JsonUtils.getMapper().registerModule(new SpanJsonModule());
		when(requestMonitorPlugin.getOnlyReportNExternalRequestsPerMinute()).thenReturn(1000000d);
		reporter = new ElasticsearchSpanReporter(requestTraceLogger);
		reporter.init(new SpanReporter.InitArguments(configuration, registry));
		when(requestMonitorPlugin.getOnlyReportRequestsWithNameToElasticsearch()).thenReturn(Collections.emptyList());
		final RequestMonitor requestMonitor = mock(RequestMonitor.class);
		when(requestMonitorPlugin.getRequestMonitor()).thenReturn(requestMonitor);
		when(requestMonitor.getRequestInformation()).thenReturn(mock(RequestMonitor.RequestInformation.class));
	}

	@Test
	public void reportRequestTrace() throws Exception {
		when(requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()).thenReturn(false);
		final Span span = getSpan();
		report(span);

		verify(elasticsearchClient).index(startsWith("stagemonitor-spans-"), eq("spans"), any());
		assertTrue(reporter.isActive(RequestMonitor.RequestInformation.of(span, null)));
		verifyTimerCreated(1);
	}

	@Test
	public void doNotReportRequestTrace() throws Exception {
		when(requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()).thenReturn(false);
		when(elasticsearchClient.isElasticsearchAvailable()).thenReturn(false);
		when(corePlugin.getElasticsearchUrl()).thenReturn(null);
		final Span span = getSpan();
		report(span);

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), any());
		verify(requestTraceLogger, times(0)).info(anyString());
		assertFalse(reporter.isActive(RequestMonitor.RequestInformation.of(span, null)));
		verifyTimerCreated(1);
	}

	@Test
	public void testLogReportRequestTrace() throws Exception {
		when(requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()).thenReturn(true);

		final Span span = requestMonitorPlugin.getTracer().buildSpan("test").start();
		report(span);

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), any());
		verify(requestTraceLogger).info(startsWith("{\"index\":{\"_index\":\"stagemonitor-spans-"));
		assertTrue(reporter.isActive(RequestMonitor.RequestInformation.of(span, null)));
	}

	@Test
	public void reportRequestTraceRateLimited() throws Exception {
		when(requestMonitorPlugin.getOnlyReportNExternalRequestsPerMinute()).thenReturn(1d);
		report(getSpan());
		verify(elasticsearchClient).index(anyString(), anyString(), any());
		Thread.sleep(5010); // the meter only updates every 5 seconds
		report(getSpan());
		verifyNoMoreInteractions(requestTraceLogger);
		verifyTimerCreated(2);
	}

	@Test
	public void excludeExternalRequestsFasterThan() throws Exception {
		when(requestMonitorPlugin.getExcludeExternalRequestsFasterThan()).thenReturn(100d);

		report(getSpan(100));
		verify(elasticsearchClient).index(anyString(), anyString(), any());

		report(getSpan(99));
		verifyNoMoreInteractions(requestTraceLogger);
		verifyTimerCreated(2);
	}

	@Test
	public void testElasticsearchExcludeFastCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeExternalRequestsWhenFasterThanXPercent()).thenReturn(0.85d);

		report(getSpan(1000));
		verify(elasticsearchClient).index(anyString(), anyString(), any());
		report(getSpan(250));
		verifyNoMoreInteractions(requestTraceLogger);
		verifyTimerCreated(2);
	}

	private void report(Span span) {
		final RequestMonitor.RequestInformation requestInformation = RequestMonitor.RequestInformation.of(span, null);
		final RequestMonitor.RequestInformation reportArguments = RequestMonitor.RequestInformation.of(span, null, Collections.<String, Object>emptyMap());
		if (reporter.isActive(requestInformation)) {
			reporter.report(reportArguments);
		}
	}

	@Test
	public void testElasticsearchDontExcludeSlowCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeExternalRequestsWhenFasterThanXPercent()).thenReturn(0.85d);

		report(getSpan(250));
		report(getSpan(1000));

		verify(elasticsearchClient, times(2)).index(anyString(), anyString(), any());
		verifyTimerCreated(2);
	}

	private Span getSpan() {
		return getSpan(1);
	}

	private Span getSpan(long executionTimeMillis) {
		final Span span = new SpanWrapper(mock(Span.class), "External Request", 1,
				Collections.singletonList(new ExternalRequestMetricsSpanInterceptor(corePlugin, requestMonitorPlugin)));
		Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_CLIENT);
		SpanUtils.setOperationType(span, "jdbc");
		span.setTag("method", "SELECT");
		span.finish(TimeUnit.MILLISECONDS.toMicros(executionTimeMillis) + 1);
		return span;
	}

	private void verifyTimerCreated(int count) {
		final Timer timer = registry.getTimers().get(name("external_request_response_time")
				.tag("type", "jdbc")
				.tag("signature", "External Request")
				.tag("method", "SELECT")
				.build());
		assertNotNull(registry.getTimers().keySet().toString(), timer);
		assertEquals(count, timer.getCount());

		final Timer allTimer = registry.getTimers().get(name("external_request_response_time")
				.tag("type", "jdbc")
				.tag("signature", "All")
				.tag("method", "SELECT")
				.build());
		assertNotNull(allTimer);
		assertEquals(count, allTimer.getCount());
	}
}
