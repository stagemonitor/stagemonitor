package org.stagemonitor.core.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.configuration.AbstractElasticsearchTest;
import org.stagemonitor.core.util.HttpClient;

import java.io.IOException;
import java.io.OutputStream;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.stagemonitor.core.elasticsearch.ElasticsearchClient.requireBoxTypeHotIfHotColdAritectureActive;

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
		assertTrue(requireBoxTypeHotIfHotColdAritectureActive("stagemonitor-elasticsearch-metrics-index-template.json", 2).contains("hot"));
	}

	@Test
	public void testDontRequireBoxTypeHotWhenHotColdInactive() throws Exception {
		assertFalse(requireBoxTypeHotIfHotColdAritectureActive("stagemonitor-elasticsearch-metrics-index-template.json", 0).contains("hot"));
		assertFalse(requireBoxTypeHotIfHotColdAritectureActive("stagemonitor-elasticsearch-metrics-index-template.json", -1).contains("hot"));
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
