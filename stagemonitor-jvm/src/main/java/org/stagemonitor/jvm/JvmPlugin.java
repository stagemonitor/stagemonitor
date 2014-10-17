package org.stagemonitor.jvm;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.rest.RestClient;

import java.util.Collections;
import java.util.List;

public class JvmPlugin implements StagemonitorPlugin {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		return Collections.emptyList();
	}

	@Override
	public void initializePlugin(MetricRegistry registry, Configuration configuration) {
		CorePlugin corePlugin = configuration.getConfig(CorePlugin.class);

		registry.register("jvm.gc", new GarbageCollectorMetricSet());
		registry.register("jvm.memory", new MemoryUsageGaugeSet());

		final CpuUtilisationWatch cpuWatch;
		try {
			cpuWatch = new CpuUtilisationWatch();
			cpuWatch.start();
			registry.register("jvm.cpu.process.usage", new Gauge<Float>() {
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
			logger.warn("Could not register cpu usage. (this exception is ignored)", e);
		}
		registry.register("online", new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return 1;
			}
		});

		RestClient.sendGrafanaDashboardAsync(corePlugin.getElasticsearchUrl(), "JVM Memory.json");
		RestClient.sendGrafanaDashboardAsync(corePlugin.getElasticsearchUrl(), "JVM Overview.json");
	}
}
