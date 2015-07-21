package org.stagemonitor;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.Timer;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.SortedTableLogReporter;

public class InstrumentationPerformanceTest  {

	private static Node node;

	public static void main(String[] args) throws Exception {
		final Timer.Context timer = Stagemonitor.getMetricRegistry().timer("startElasticsearch").time();
		startElasticsearch();
		Stagemonitor.init();
		timer.stop();
		printResults();
		node.close();
	}

	private static void startElasticsearch() {
		try {
			FileUtils.deleteDirectory(new File("build/elasticsearch"));
		} catch (IOException e) {
			// ignore
		}
		final NodeBuilder nodeBuilder = NodeBuilder.nodeBuilder().local(true);
		nodeBuilder.settings()
				.put("name", "junit-es-node")
				.put("node.http.enabled", "false")
				.put("path.logs", "build/elasticsearch/logs")
				.put("path.data", "build/elasticsearch/data")
				.put("index.store.fs.memory.enabled", "true")
				.put("index.gateway.type", "none")
				.put("gateway.type", "none")
				.put("index.store.type", "memory")
				.put("index.number_of_shards", "1")
				.put("index.number_of_replicas", "0")
				.put("discovery.zen.ping.multicast.enabled", "false");

		node = nodeBuilder.node();
		node.client().admin().cluster().prepareHealth().setWaitForGreenStatus().get();
	}

	public static void printResults() throws Exception {
		SortedTableLogReporter reporter = SortedTableLogReporter
				.forRegistry(Stagemonitor.getMetricRegistry())
				.log(LoggerFactory.getLogger(InstrumentationPerformanceTest.class))
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS)
				.filter(MetricFilter.ALL)
				.formattedFor(Locale.US)
				.build();
		reporter.report(new TreeMap<String, Gauge>(), new TreeMap<String, Counter>(),
				new TreeMap<String, Histogram>(), new TreeMap<String, Meter>(),
				Stagemonitor.getMetricRegistry().getTimers());

		Stagemonitor.reset();
	}

}
