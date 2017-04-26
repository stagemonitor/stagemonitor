package org.stagemonitor.core.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.stagemonitor.AbstractElasticsearchTest;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.JsonUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.stagemonitor.core.elasticsearch.ElasticsearchClient.modifyIndexTemplate;

public class ElasticsearchClientTest extends AbstractElasticsearchTest {

	private ListAppender<ILoggingEvent> testAppender;

	@Before
	public void setUp() throws Exception {
		Logger bulkLogger = (Logger) LoggerFactory.getLogger(ElasticsearchClient.BulkErrorReportingResponseHandler.class);
		testAppender = new ListAppender<ILoggingEvent>();
		testAppender.start();
		bulkLogger.addAppender(testAppender);
	}

	@After
	public void tearDown() throws Exception {
		Logger bulkLogger = (Logger) LoggerFactory.getLogger(ElasticsearchClient.BulkErrorReportingResponseHandler.class);
		bulkLogger.detachAppender(testAppender);
	}

	@Test
	public void testSendDashboard() throws Exception {
		elasticsearchClient.sendGrafana1DashboardAsync("Test Dashboard.json").get();
		refresh();
		final JsonNode dashboard = elasticsearchClient.getJson("/grafana-dash/dashboard/test-title");
		assertEquals("test-title", dashboard.get("_id").textValue());
	}

	@Test
	public void testGetDashboardForElasticsearch() throws Exception {
		String expected = "{\"user\":\"guest\",\"group\":\"guest\",\"title\":\"Test Title\",\"tags\":[\"jdbc\",\"db\"],\"dashboard\":\"{\\\"title\\\":\\\"Test Title\\\",\\\"editable\\\":false,\\\"failover\\\":false,\\\"panel_hints\\\":true,\\\"style\\\":\\\"dark\\\",\\\"refresh\\\":\\\"1m\\\",\\\"tags\\\":[\\\"jdbc\\\",\\\"db\\\"],\\\"timezone\\\":\\\"browser\\\"}\"}";
		assertEquals(expected, elasticsearchClient.getDashboardForElasticsearch("Test Dashboard.json").toString());
	}

	@Test
	public void testRequireBoxTypeHotWhenHotColdActive() throws Exception {
		final String indexTemplate = modifyIndexTemplate("stagemonitor-elasticsearch-metrics-index-template.json", 2, null, 0);
		assertTrue(indexTemplate.contains("hot"));
		assertFalse(indexTemplate.contains("number_of_shards"));
		assertFalse(indexTemplate.contains("number_of_replicas"));
	}

	@Test
	public void testSetReplicas() throws Exception {
		final String indexTemplate = modifyIndexTemplate("stagemonitor-elasticsearch-metrics-index-template.json", 0, 0, 0);
		assertFalse(indexTemplate.contains("hot"));
		assertEquals(0, JsonUtils.getMapper().readTree(indexTemplate).get("settings").get("index").get("number_of_replicas").asInt());
		assertFalse(indexTemplate.contains("number_of_shards"));
	}

	@Test
	public void testSetShards() throws Exception {
		final String indexTemplate = modifyIndexTemplate("stagemonitor-elasticsearch-metrics-index-template.json", 0, -1, 1);
		assertFalse(indexTemplate.contains("hot"));
		assertEquals(1, JsonUtils.getMapper().readTree(indexTemplate).get("settings").get("index").get("number_of_shards").asInt());
		assertFalse(indexTemplate.contains("number_of_replicas"));
	}

	@Test
	public void modifyIndexTemplateIntegrationTest() throws Exception {
		elasticsearchClient.sendMappingTemplateAsync(modifyIndexTemplate("stagemonitor-elasticsearch-metrics-index-template.json", 0, 1, 2), "stagemonitor-elasticsearch-metrics");
		elasticsearchClient.waitForCompletion();
		refresh();
		elasticsearchClient.index("stagemonitor-metrics-test", "metrics", Collections.singletonMap("count", 1));
		elasticsearchClient.waitForCompletion();
		refresh();
		final JsonNode indexSettings = elasticsearchClient.getJson("/stagemonitor-metrics-test/_settings")
				.get("stagemonitor-metrics-test").get("settings").get("index");
		assertEquals(indexSettings.toString(),1, indexSettings.get("number_of_replicas").asInt());
		assertEquals(indexSettings.toString(),2, indexSettings.get("number_of_shards").asInt());
	}

	@Test
	public void testDontRequireBoxTypeHotWhenHotColdInactive() throws Exception {
		assertFalse(modifyIndexTemplate("stagemonitor-elasticsearch-metrics-index-template.json", 0, 0, 0).contains("hot"));
		assertFalse(modifyIndexTemplate("stagemonitor-elasticsearch-metrics-index-template.json", -1, 0, 0).contains("hot"));
	}

	@Test
	public void testBulkNoRequest() {
		elasticsearchClient.sendBulk("", new HttpClient.OutputStreamHandler() {
			@Override
			public void withHttpURLConnection(OutputStream os) throws IOException {
				os.write(("").getBytes("UTF-8"));
			}
		});
		assertThat(testAppender.list.size(), is(1));
		final ILoggingEvent event = testAppender.list.get(0);
		assertThat(event.getLevel().toString(), is("WARN"));
		assertThat(event.getMessage(), startsWith("Error(s) while sending a _bulk request to elasticsearch: {}"));
	}

	@Test
	public void testBulkErrorInvalidRequest() {
		elasticsearchClient.sendBulk("", new HttpClient.OutputStreamHandler() {
			@Override
			public void withHttpURLConnection(OutputStream os) throws IOException {
				os.write(("{ \"update\" : {\"_id\" : \"1\", \"_type\" : \"type1\", \"_index\" : \"index1\", \"_retry_on_conflict\" : 3} }\n" +
						"{ \"doc\" : {\"field\" : \"value\"} }\n").getBytes("UTF-8"));
			}
		});
		assertThat(testAppender.list.toString(), testAppender.list.size(), is(1));
		final ILoggingEvent event = testAppender.list.get(0);
		assertThat(event.getLevel().toString(), is("WARN"));
		assertThat(event.getMessage(), startsWith("Error(s) while sending a _bulk request to elasticsearch: {}"));
	}

	@Test
	public void testSuccessfulBulkRequest() {
		elasticsearchClient.sendBulk("", new HttpClient.OutputStreamHandler() {
			@Override
			public void withHttpURLConnection(OutputStream os) throws IOException {
				os.write(("{ \"index\" : { \"_index\" : \"test\", \"_type\" : \"type1\", \"_id\" : \"1\" } }\n" +
						"{ \"field1\" : \"value1\" }\n").getBytes("UTF-8"));
			}
		});
		assertThat(testAppender.list.size(), is(0));
	}

}
