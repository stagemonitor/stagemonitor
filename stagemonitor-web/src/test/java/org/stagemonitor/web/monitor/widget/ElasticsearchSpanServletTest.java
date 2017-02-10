package org.stagemonitor.web.monitor.widget;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.requestmonitor.MonitoredMethodRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.reporter.ElasticsearchSpanReporterIntegrationTest;
import org.stagemonitor.web.WebPlugin;

import java.util.Collections;

import io.opentracing.Span;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticsearchSpanServletTest extends ElasticsearchSpanReporterIntegrationTest {

	private ElasticsearchSpanServlet elasticsearchSpanServlet;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		when(configuration.getConfig(WebPlugin.class)).thenReturn(mock(WebPlugin.class));
		elasticsearchSpanServlet = new ElasticsearchSpanServlet(configuration);
	}

	@Test
	public void testSpanServlet() throws Exception {
		final Span span = new MonitoredMethodRequest(configuration, "Test#test", null, Collections.singletonMap("attr.Color", "Blue")).createSpan();
		reporter.report(RequestMonitor.RequestInformation.of(span, null, Collections.<String, Object>emptyMap()));
		elasticsearchClient.waitForCompletion();
		refresh();
		final MockHttpServletRequest req = new MockHttpServletRequest();
		final String spanId = String.format("%x", ((com.uber.jaeger.Span) span).context().getSpanID());
		req.addParameter("id", spanId);
		final MockHttpServletResponse resp = new MockHttpServletResponse();
		elasticsearchSpanServlet.doGet(req, resp);
		assertTrue(resp.getContentAsString().contains("\"id\":\"" + spanId + "\""));
		assertFalse(resp.getContentAsString().contains("_index"));
	}
}
