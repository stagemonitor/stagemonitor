package org.stagemonitor;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.Netty4Plugin;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.TimedElementMatcherDecorator;
import org.stagemonitor.core.metrics.SortedTableLogReporter;
import org.stagemonitor.core.metrics.metrics2.MetricName;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class InstrumentationPerformanceTest  {

	private static Node node;

	private InstrumentationPerformanceTest() {
	}

	public static void main(String[] args) throws Exception {
		final Timer.Context timer = Stagemonitor.getMetric2Registry().timer(name("startElasticsearch").build()).time();
		startElasticsearch();
		Stagemonitor.init();
		timer.stop();
		printResults();
		node.close();
	}

	private static void startElasticsearch() throws Exception {
		try {
			FileUtils.deleteDirectory(new File("build/elasticsearch"));
		} catch (IOException e) {
			// ignore
		}
		final Settings settings = Settings.builder()
				.put("path.home", "build/elasticsearch")
				.put("node.name", "junit-es-node")
				.put("path.logs", "build/elasticsearch/logs")
				.put("path.data", "build/elasticsearch/data")
				.put("transport.type", "local")
				.put("http.type", "netty4")
				.build();

		node = new TestNode(settings, Collections.singletonList(Netty4Plugin.class));
		node.start();
		node.client().admin().cluster().prepareHealth().setWaitForGreenStatus().get();
	}

	private static class TestNode extends Node {
		public TestNode(Settings preparedSettings, Collection<Class<? extends Plugin>> classpathPlugins) {
			super(InternalSettingsPreparer.prepareEnvironment(preparedSettings, null), classpathPlugins);
		}
	}

	public static void printResults() throws Exception {
		SortedTableLogReporter reporter = SortedTableLogReporter
				.forRegistry(Stagemonitor.getMetric2Registry())
				.log(LoggerFactory.getLogger(InstrumentationPerformanceTest.class))
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS)
				.formattedFor(Locale.US)
				.build();
		reporter.reportMetrics(new HashMap<MetricName, Gauge>(), new HashMap<MetricName, Counter>(),
				new HashMap<MetricName, Histogram>(), new HashMap<MetricName, Meter>(),
				Stagemonitor.getMetric2Registry().getTimers());

		TimedElementMatcherDecorator.logMetrics();
		Stagemonitor.reset();
	}

}
