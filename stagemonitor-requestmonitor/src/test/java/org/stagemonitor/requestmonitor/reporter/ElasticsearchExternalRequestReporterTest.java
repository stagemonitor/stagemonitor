package org.stagemonitor.requestmonitor.reporter;


import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.requestmonitor.ExternalRequest;
import org.stagemonitor.requestmonitor.RequestTrace;

public class ElasticsearchExternalRequestReporterTest extends ElasticsearchRequestTraceReporterTest {

	private ElasticsearchExternalRequestReporter elasticsearchExternalRequestReporter;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		elasticsearchExternalRequestReporter = new ElasticsearchExternalRequestReporter(requestTraceLogger);
		elasticsearchExternalRequestReporter.init(new RequestTraceReporter.InitArguments(configuration));
	}

	@Test
	public void reportRequestTrace() throws Exception {
		elasticsearchExternalRequestReporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(getRequestTrace()));

		verify(elasticsearchClient).sendBulk(anyString(), any());
		assertTrue(elasticsearchExternalRequestReporter.isActive(new RequestTraceReporter.IsActiveArguments(getRequestTrace())));
	}

	@Test
	public void testLogReportRequestTrace() throws Exception {
		when(requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()).thenReturn(true);
		elasticsearchExternalRequestReporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(getRequestTrace()));

		verify(elasticsearchClient, times(0)).index(anyString(), anyString(), anyObject());
		verify(requestTraceLogger).info(startsWith("{\"index\":{}}\n{"));
		assertTrue(elasticsearchExternalRequestReporter.isActive(new RequestTraceReporter.IsActiveArguments(getRequestTrace())));
	}

	private RequestTrace getRequestTrace() {
		final RequestTrace requestTrace = new RequestTrace("abc", new MeasurementSession(getClass().getName(), "test", "test"), requestMonitorPlugin);
		requestTrace.setName("Report Me");
		requestTrace.addExternalRequest(new ExternalRequest("jdbc", "SELECT", 1000000, "SELECT * from STAGEMONITOR"));
		return requestTrace;
	}

}