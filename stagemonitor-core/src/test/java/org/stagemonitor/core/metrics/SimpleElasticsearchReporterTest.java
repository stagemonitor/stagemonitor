package org.stagemonitor.core.metrics;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.counter;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.gauge;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.histogram;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.map;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.meter;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.snapshot;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.timer;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.AbstractElasticsearchTest;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.JsonUtils;

public class SimpleElasticsearchReporterTest extends AbstractElasticsearchTest {

	private SimpleElasticsearchReporter reporter;

	@BeforeClass
	public static void setup() throws Exception {
		Configuration configuration = mock(Configuration.class);
		CorePlugin corePlugin = mock(CorePlugin.class);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient);
		when(corePlugin.getAggregationReportingInterval()).thenReturn(30L);
		new CorePlugin().initializePlugin(new MetricRegistry(), configuration);
		while (!elasticsearchClient.asyncRestPool.getQueue().isEmpty()) {
			// give the async tasks time to complete
			Thread.sleep(10);
		}
		Thread.sleep(500);
		refresh();
	}

	@Before
	public void setUp() throws Exception {
		reporter = new SimpleElasticsearchReporter(elasticsearchClient, null, null, MetricFilter.ALL);
	}

	@Test
	public void testReport() throws Exception {
		reporter.report(map("gauge", gauge(1.1)).add("stringGauge", gauge("test")), map("counter", counter(1)), map("histogram", histogram(4)),
				map("meter", meter(4)),
				map("timer", timer(MILLISECONDS.toNanos(1L), MILLISECONDS.toNanos(2), MILLISECONDS.toNanos(3),
						MILLISECONDS.toNanos(4), MILLISECONDS.toNanos(5), snapshot(MILLISECONDS.toNanos(4),
						MILLISECONDS.toNanos(2), MILLISECONDS.toNanos(4), MILLISECONDS.toNanos(5), MILLISECONDS.toNanos(6),
						MILLISECONDS.toNanos(7), MILLISECONDS.toNanos(8), MILLISECONDS.toNanos(9),
						MILLISECONDS.toNanos(10), MILLISECONDS.toNanos(11)))));

		refresh();

		final SearchResponse searchResponse = client.prepareSearch("stagemonitor").setTypes("measurementSessions").get();
		assertEquals(1, searchResponse.getHits().totalHits());
		assertEquals(JsonUtils.getMapper().readTree(String.format("{\"applicationName\":null," +
				"\"hostName\":null," +
				"\"instanceName\":null," +
				"\"startTimestamp\":%d," +
				"\"endTimestamp\":null," +
				"\"start\":\"%s\"," +
				"\"end\":null," +
				"\"gauges\":{\"gauge\":{\"value\":1.1},\"stringGauge\":{\"value\":\"test\"}}," +
				"\"counters\":{\"counter\":{\"count\":1}}," +
				"\"histograms\":{\"histogram\":{\"count\":1,\"max\":2,\"mean\":4.0,\"min\":4,\"p50\":6.0,\"p75\":7.0,\"p95\":8.0,\"p98\":9.0,\"p99\":10.0,\"p999\":11.0,\"stddev\":5.0}}," +
				"\"meters\":{\"meter\":{\"count\":4,\"m15_rate\":5.0,\"m1_rate\":3.0,\"m5_rate\":4.0,\"mean_rate\":2.0,\"units\":\"events/second\"}}," +
				"\"timers\":{\"timer\":{\"count\":1000000,\"max\":2.0,\"mean\":4.0,\"min\":4.0,\"p50\":6.0,\"p75\":7.0,\"p95\":8.0,\"p98\":9.0,\"p99\":10.0,\"p999\":11.0,\"stddev\":5.0,\"m15_rate\":5000000.0,\"m1_rate\":3000000.0,\"m5_rate\":4000000.0,\"mean_rate\":2000000.0,\"duration_units\":\"milliseconds\",\"rate_units\":\"calls/second\"}}}",
				Stagemonitor.getMeasurementSession().getStartTimestamp(), Stagemonitor.getMeasurementSession().getStart())),
				JsonUtils.getMapper().readTree(searchResponse.getHits().getAt(0).getSourceAsString()));
	}

	@Test
	public void testMapping() throws Exception {
		final GetMappingsResponse mappings = client.admin().indices().prepareGetMappings("stagemonitor").setTypes("measurementSessions").get();
		assertEquals(1, mappings.getMappings().size());
		assertEquals("{\"measurementSessions\":" +
				"{\"dynamic_templates\":" +
				"[{\"fields\":" +
				"{\"mapping\":" +
				"{\"index\":\"not_analyzed\",\"type\":\"string\"},\"match\":\"*\",\"match_mapping_type\":\"string\"}}]," +
				"\"_all\":{\"enabled\":false}," +
				"\"properties\":" +
				"{\"endTimestamp\":{\"type\":\"date\",\"format\":\"dateOptionalTime\"}," +
				"\"startTimestamp\":{\"type\":\"date\",\"format\":\"dateOptionalTime\"}}}}",
				mappings.getMappings().get("stagemonitor").get("measurementSessions").source().toString());
	}
}
