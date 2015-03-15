package org.stagemonitor.core.metrics;


import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.JsonUtils;

/**
 * This implementation is intended to report aggregate metrics about a measurement session to elasticsearch on shutdown
 */
public class SimpleElasticsearchReporter extends ScheduledReporter {

	private final ElasticsearchClient elasticsearchClient;

	public SimpleElasticsearchReporter(ElasticsearchClient elasticsearchClient, MetricRegistry registry, String name, MetricFilter filter) {
		super(registry, name, filter, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
		this.elasticsearchClient = elasticsearchClient;
	}

	@Override
	public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
					   SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
		final ObjectMapper mapper = JsonUtils.getMapper();
		final ObjectNode jsonReport = mapper.valueToTree(Stagemonitor.getMeasurementSession());
		jsonReport.set("gauges", mapper.valueToTree(gauges));
		jsonReport.set("counters", mapper.valueToTree(counters));
		jsonReport.set("histograms", mapper.valueToTree(histograms));
		jsonReport.set("meters", mapper.valueToTree(meters));
		jsonReport.set("timers", mapper.valueToTree(timers));

		elasticsearchClient.sendAsJson("POST", "/stagemonitor/measurementSessions", jsonReport);
	}
}
