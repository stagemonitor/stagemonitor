package org.stagemonitor.web.monitor.widget;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.requestmonitor.reporter.ElasticsearchRequestTraceReporterIntegrationTest;
import org.stagemonitor.requestmonitor.reporter.SpanReporter;
import org.stagemonitor.web.WebPlugin;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticsearchRequestTraceServletTest extends ElasticsearchRequestTraceReporterIntegrationTest {

	private ElasticsearchRequestTraceServlet elasticsearchRequestTraceServlet;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		when(configuration.getConfig(WebPlugin.class)).thenReturn(mock(WebPlugin.class));
		elasticsearchRequestTraceServlet = new ElasticsearchRequestTraceServlet(configuration);
	}

	@Test
	public void testRequestTraceServlet() throws Exception {
		final RequestTrace requestTrace = new RequestTrace("1", new MeasurementSession("ElasticsearchRequestTraceServletTest", "test", "test"), requestMonitorPlugin);
		requestTrace.setParameters(Collections.singletonMap("attr.Color", "Blue"));
		reporter.report(new SpanReporter.ReportArguments(requestTrace, null));
		elasticsearchClient.waitForCompletion();
		refresh();
		final MockHttpServletRequest req = new MockHttpServletRequest();
		req.addParameter("id", "1");
		final MockHttpServletResponse resp = new MockHttpServletResponse();
		elasticsearchRequestTraceServlet.doGet(req, resp);
		assertTrue(resp.getContentAsString().contains("\"id\":\"1\""));
		assertFalse(resp.getContentAsString().contains("_index"));
	}
}
