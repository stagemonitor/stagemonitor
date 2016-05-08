package org.stagemonitor.requestmonitor.reporter;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.stagemonitor.requestmonitor.RequestMonitor.getExternalRequestTimerName;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.requestmonitor.ExternalRequest;
import org.stagemonitor.requestmonitor.RequestTrace;

public class ElasticsearchExternalRequestReporterTest extends AbstractElasticsearchRequestTraceReporterTest {

	private ElasticsearchExternalRequestReporter reporter;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		when(requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()).thenReturn(true);
		when(requestMonitorPlugin.getOnlyReportNExternalRequestsPerMinute()).thenReturn(1000000d);
		reporter = new ElasticsearchExternalRequestReporter(requestTraceLogger);
		reporter.init(new RequestTraceReporter.InitArguments(configuration));
	}

	@Test
	public void reportRequestTrace() throws Exception {
		when(requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()).thenReturn(false);
		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(getRequestTrace()));

		verify(elasticsearchClient).sendBulkAsync(anyString(), any());
		assertTrue(reporter.isActive(new RequestTraceReporter.IsActiveArguments(getRequestTrace())));
	}

	@Test
	public void testLogReportRequestTrace() throws Exception {
		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(getRequestTrace()));

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), anyObject());
		verify(requestTraceLogger).info(startsWith("{\"index\":{}}\n{"));
		assertTrue(reporter.isActive(new RequestTraceReporter.IsActiveArguments(getRequestTrace())));
	}

	private RequestTrace getRequestTrace() {
		return getRequestTrace(1);
	}

	private RequestTrace getRequestTrace(long executionTimeMillis) {
		final RequestTrace requestTrace = new RequestTrace("abc", new MeasurementSession(getClass().getName(), "test", "test"), requestMonitorPlugin);
		requestTrace.setName("Report Me");
		final ExternalRequest externalRequest = new ExternalRequest("jdbc", "SELECT", TimeUnit.MILLISECONDS.toNanos(executionTimeMillis), "SELECT * from STAGEMONITOR");
		externalRequest.setExecutedBy("ElasticsearchExternalRequestReporterTest#test");
		requestTrace.addExternalRequest(externalRequest);
		registry
				.timer(getExternalRequestTimerName(externalRequest))
				.update(executionTimeMillis, TimeUnit.MILLISECONDS);
		return requestTrace;
	}

	@Test
	public void reportRequestTraceRateLimited() throws Exception {
		when(requestMonitorPlugin.getOnlyReportNExternalRequestsPerMinute()).thenReturn(1d);
		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(getRequestTrace()));
		verify(requestTraceLogger).info(anyString());
		Thread.sleep(5010); // the meter only updates every 5 seconds
		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(getRequestTrace()));
		verifyNoMoreInteractions(requestTraceLogger);
	}

	@Test
	public void excludeExternalRequestsFasterThan() throws Exception {
		when(requestMonitorPlugin.getExcludeExternalRequestsFasterThan()).thenReturn(100d);

		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(getRequestTrace(100)));
		verify(requestTraceLogger).info(anyString());

		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(getRequestTrace(99)));
		verifyNoMoreInteractions(requestTraceLogger);
	}

	@Test
	public void testElasticsearchExcludeFastCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeExternalRequestsWhenFasterThanXPercent()).thenReturn(0.85d);

		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(getRequestTrace(1000)));
		verify(requestTraceLogger).info(anyString());
		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(getRequestTrace(250)));
		verifyNoMoreInteractions(requestTraceLogger);
	}

	@Test
	public void testElasticsearchDontExcludeSlowCallTree() throws Exception {
		when(requestMonitorPlugin.getExcludeExternalRequestsWhenFasterThanXPercent()).thenReturn(0.85d);

		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(getRequestTrace(250)));
		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(getRequestTrace(1000)));

		verify(requestTraceLogger, times(2)).info(anyString());
	}
}