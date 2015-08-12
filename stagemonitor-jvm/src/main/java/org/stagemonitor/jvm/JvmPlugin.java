package org.stagemonitor.jvm;


import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.Collections;
import java.util.List;

import com.codahale.metrics.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

public class JvmPlugin extends StagemonitorPlugin {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void initializePlugin(Metric2Registry registry, Configuration configuration) {
		// TODO
//		registry.register(name("jvm_gc").build(), new GarbageCollectorMetricSet());
//		registry.register(name("jvm_memory").build(), new MemoryUsageGaugeSet());

		final CpuUtilisationWatch cpuWatch;
		try {
			cpuWatch = new CpuUtilisationWatch();
			cpuWatch.start();
			registry.register(name("jvm_process_cpu_usage").build(), new Gauge<Float>() {
				@Override
				public Float getValue() {
					try {
						return cpuWatch.getCpuUsagePercent();
					} finally {
						cpuWatch.start();
					}
				}
			});
		} catch (Exception e) {
			logger.warn("Could not register cpu usage. ({})", e.getMessage());
		}

		ElasticsearchClient elasticsearchClient = configuration.getConfig(CorePlugin.class).getElasticsearchClient();
		elasticsearchClient.sendGrafanaDashboardAsync("JVM Memory.json");
		elasticsearchClient.sendGrafanaDashboardAsync("JVM Overview.json");
	}

	@Override
	public List<String> getPathsOfWidgetMetricTabPlugins() {
		return Collections.singletonList("/stagemonitor/static/tabs/metrics/jvm-metrics");
	}
}
