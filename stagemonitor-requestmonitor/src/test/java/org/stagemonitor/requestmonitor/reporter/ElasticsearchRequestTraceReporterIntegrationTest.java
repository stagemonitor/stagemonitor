package org.stagemonitor.requestmonitor.reporter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.configuration.AbstractElasticsearchTest;
import org.stagemonitor.requestmonitor.*;

public class ElasticsearchRequestTraceReporterIntegrationTest extends AbstractElasticsearchTest {

	private org.stagemonitor.requestmonitor.ElasticsearchRequestTraceReporter reporter;

	@Before
	public void setUp() throws Exception {
		final CorePlugin corePlugin = mock(CorePlugin.class);
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient);
		final RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(1000000d);
		reporter =  new org.stagemonitor.requestmonitor.ElasticsearchRequestTraceReporter(corePlugin, requestMonitorPlugin);
	}

	@Test
	public void reportRequestTrace() throws Exception {
		final RequestTrace requestTrace = new RequestTrace("1", new MeasurementSession("ERTRIT", "test", "test"));
		requestTrace.setParameters(Collections.singletonMap("attr.Color", "Blue"));
		requestTrace.addCustomProperty("foo.bar", "baz");
		reporter.reportRequestTrace(requestTrace);
		elasticsearchClient.waitForCompletion();
		refresh();
		final JsonNode hits = elasticsearchClient.getJson("/stagemonitor-requests*/_search").get("hits");
		assertEquals(1, hits.get("total").intValue());
		assertEquals("Blue", hits.get("hits").elements().next().get("_source").get("parameters").get("attr_(dot)_Color").asText());
		assertEquals("baz", hits.get("hits").elements().next().get("_source").get("foo_(dot)_bar").asText());
	}
}