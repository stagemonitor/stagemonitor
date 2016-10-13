package org.stagemonitor.requestmonitor.reporter;

import com.codahale.metrics.Timer;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.requestmonitor.ExternalRequest;
import org.stagemonitor.requestmonitor.RequestTrace;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.stagemonitor.requestmonitor.reporter.ExternalRequestMetricsReporter.getExternalRequestTimerName;

public class ElasticsearchExternalRequestReporterTest extends AbstractElasticsearchRequestTraceReporterTest {

	private ElasticsearchExternalRequestReporter reporter;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		when(requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()).thenReturn(true);
		when(requestMonitorPlugin.getOnlyReportNExternalRequestsPerMinute()).thenReturn(1000000d);
		reporter = new ElasticsearchExternalRequestReporter(requestTraceLogger);
		reporter.init(new SpanReporter.InitArguments(configuration));
	}

	@Test
	public void reportRequestTrace() throws Exception {
		when(requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()).thenReturn(false);
		reporter.report(new SpanReporter.ReportArguments(getRequestTrace(), null));

		verify(elasticsearchClient).sendBulkAsync(anyString(), any());
		assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(getRequestTrace(), null)));
		verifyTimerCreated(1);
	}

	@Test
	public void doNotReportRequestTrace() throws Exception {
		when(requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()).thenReturn(false);
		when(corePlugin.getElasticsearchUrls()).thenReturn(Collections.emptyList());
		when(corePlugin.getElasticsearchUrl()).thenReturn(null);
		reporter.report(new SpanReporter.ReportArguments(getRequestTrace(), null));

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), anyObject());
		verify(requestTraceLogger, times(0)).info(anyString());
		assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(getRequestTrace(), null)));
		verifyTimerCreated(1);
	}

	@Test
	public void testLogReportRequestTrace() throws Exception {
		reporter.report(new SpanReporter.ReportArguments(getRequestTrace(), null));

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), anyObject());
		verify(requestTraceLogger).info(startsWith("{\"index\":{}}\n{"));
		assertTrue(reporter.isActive(new SpanReporter.IsActiveArguments(getRequestTrace(), null)));
		verifyTimerCreated(1);
	}

	@Test
	public void reportRequestTraceRateLimited() throws Exception {
		when(requestMonitorPlugin.getOnlyReportNExternalRequestsPerMinute()).thenReturn(1d);
		reporter.report(new SpanReporter.ReportArguments(getRequestTrace(), null));
		verify(requestTraceLogger).info(anyString());
		Thread.sleep(5010); // the meter only updates every 5 seconds
		reporter.report(new SpanReporter.ReportArguments(getRequestTrace(), null));
		verifyNoMoreInteractions(requestTraceLogger);
		verifyTimerCreated(2);
	}

	@Test
	public void excludeExternalRequestsFasterThan() throws Exception {
		when(requestMonitorPlugin.getExcludeExternalRequestsFasterThan()).thenReturn(100d);

		reporter.report(new SpanReporter.ReportArguments(getRequestTrace(100), null));
		verify(requestTraceLogger).info(anyString());

		reporter.report(new SpanReporter.ReportArguments(getRequestTrace(99), null));
		verifyNoMoreInteractions(requestTraceLogger);
		verifyTimerCreated(2);
	}

	@Test
	public void testElasticsearchExcludeFastCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeExternalRequestsWhenFasterThanXPercent()).thenReturn(0.85d);

		reporter.report(new SpanReporter.ReportArguments(getRequestTrace(1000), null));
		verify(requestTraceLogger).info(anyString());
		reporter.report(new SpanReporter.ReportArguments(getRequestTrace(250), null));
		verifyNoMoreInteractions(requestTraceLogger);
		verifyTimerCreated(2);
	}

	@Test
	public void testElasticsearchDontExcludeSlowCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeExternalRequestsWhenFasterThanXPercent()).thenReturn(0.85d);

		reporter.report(new SpanReporter.ReportArguments(getRequestTrace(250), null));
		reporter.report(new SpanReporter.ReportArguments(getRequestTrace(1000), null));

		verify(requestTraceLogger, times(2)).info(anyString());
		verifyTimerCreated(2);
	}

	private RequestTrace getRequestTrace() {
		return getRequestTrace(1);
	}

	private RequestTrace getRequestTrace(long executionTimeMillis) {
		final RequestTrace requestTrace = new RequestTrace("abc", new MeasurementSession(getClass().getName(), "test", "test"), requestMonitorPlugin);
		requestTrace.setName("Report Me");
		final ExternalRequest externalRequest = getTestExternalRequest(executionTimeMillis);
		requestTrace.addExternalRequest(externalRequest);
		return requestTrace;
	}

	private ExternalRequest getTestExternalRequest(long executionTimeMillis) {
		final ExternalRequest externalRequest = new ExternalRequest("jdbc", "SELECT", TimeUnit.MILLISECONDS.toNanos(executionTimeMillis), "SELECT * from STAGEMONITOR", "foo@jdbc:bar");
		externalRequest.setExecutedBy("ElasticsearchExternalRequestReporterTest#test");
		return externalRequest;
	}

	private void verifyTimerCreated(int count) {
		final ExternalRequest externalRequest = getTestExternalRequest(1);
		final Timer timer = registry.getTimers().get(getExternalRequestTimerName(externalRequest));
		assertNotNull(timer);
		assertEquals(count, timer.getCount());

		final Timer allTimer = registry.getTimers().get(getExternalRequestTimerName(externalRequest, "All"));
		assertNotNull(allTimer);
		assertEquals(count, allTimer.getCount());
	}
}
