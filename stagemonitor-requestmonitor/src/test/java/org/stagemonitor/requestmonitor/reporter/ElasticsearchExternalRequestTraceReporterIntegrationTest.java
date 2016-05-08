package org.stagemonitor.requestmonitor.reporter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.configuration.AbstractElasticsearchTest;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.IOUtils;
import org.stagemonitor.requestmonitor.ExternalRequest;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;

public class ElasticsearchExternalRequestTraceReporterIntegrationTest extends AbstractElasticsearchTest {

	protected ElasticsearchExternalRequestReporter reporter;
	protected RequestMonitorPlugin requestMonitorPlugin;
	protected Configuration configuration;

	@Before
	public void setUp() throws Exception {
		this.configuration = mock(Configuration.class);
		this.requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient);
		when(corePlugin.getMetricRegistry()).thenReturn(new Metric2Registry());
		when(requestMonitorPlugin.getOnlyReportNExternalRequestsPerMinute()).thenReturn(1000000d);
		reporter = new ElasticsearchExternalRequestReporter();
		reporter.init(new RequestTraceReporter.InitArguments(configuration));
		final String mappingTemplate = IOUtils.getResourceAsString("stagemonitor-elasticsearch-external-requests-index-template.json");
		elasticsearchClient.sendMappingTemplateAsync(mappingTemplate, "stagemonitor-external-requests");
		elasticsearchClient.waitForCompletion();
	}

	@Test
	public void reportTemplateCreated() throws Exception {
		final JsonNode template = elasticsearchClient.getJson("/_template/stagemonitor-external-requests").get("stagemonitor-external-requests");
		assertEquals("stagemonitor-external-requests-*", template.get("template").asText());
		assertEquals(false, template.get("mappings").get("_default_").get("_all").get("enabled").asBoolean());
	}

	@Test
	public void reportRequestTrace() throws Exception {
		final RequestTrace requestTrace = new RequestTrace("abc", new MeasurementSession(getClass().getName(), "test", "test"), requestMonitorPlugin);
		requestTrace.setName("Report Me");
		final ExternalRequest externalRequest = new ExternalRequest("jdbc", "SELECT", 1000000, "SELECT * from STAGEMONITOR");
		externalRequest.setExecutedBy("ElasticsearchExternalRequestTraceReporterIntegrationTest#test");
		requestTrace.addExternalRequest(externalRequest);
		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(requestTrace));
		elasticsearchClient.waitForCompletion();
		refresh();
		final JsonNode hits = elasticsearchClient.getJson("/stagemonitor-external-requests*/_search").get("hits");
		assertEquals(1, hits.get("total").intValue());
		final JsonNode requestTraceJson = hits.get("hits").elements().next().get("_source");
		assertEquals("jdbc", requestTraceJson.get("requestType").asText());
		assertEquals("SELECT", requestTraceJson.get("requestMethod").asText());
		assertEquals(1.0d, requestTraceJson.get("executionTime").asDouble(), 0.0000001);
		assertEquals("SELECT * from STAGEMONITOR", requestTraceJson.get("request").asText());
		assertEquals("ElasticsearchExternalRequestTraceReporterIntegrationTest#test", requestTraceJson.get("executedBy").asText());
	}
}