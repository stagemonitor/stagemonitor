package org.stagemonitor.collector.jvm;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.collector.core.Configuration;
import org.stagemonitor.collector.core.StageMonitorPlugin;
import org.stagemonitor.collector.core.rest.RestClient;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class JvmPlugin implements StageMonitorPlugin {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Override
	public void initializePlugin(MetricRegistry registry, Configuration configuration) {
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

		try {
			final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.newPlatformMXBeanProxy(
					ManagementFactory.getPlatformMBeanServer(), ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
					OperatingSystemMXBean.class);
			if (operatingSystemMXBean.getSystemLoadAverage() > 0.0d) {
				registry.register("os.cpu.queueLength", new Gauge<Double>() {
					@Override
					public Double getValue() {
						return Math.max(0.0, operatingSystemMXBean.getSystemLoadAverage() - operatingSystemMXBean.getAvailableProcessors());
					}
				});
				registry.register("os.cpu.load", new Gauge<Double>() {
					@Override
					public Double getValue() {
						return operatingSystemMXBean.getSystemLoadAverage();
					}
				});
			}
			registry.register("os.cpu.count", new Gauge<Integer>() {
				@Override
				public Integer getValue() {
					return operatingSystemMXBean.getAvailableProcessors();
				}
			});
		} catch (IOException e) {
			logger.warn(e.getMessage() + " (this exception is ignored)", e);
		}

		RestClient.sendGrafanaDashboardAsync(configuration.getServerUrl(), "JVM Memory.json");
		RestClient.sendGrafanaDashboardAsync(configuration.getServerUrl(), "JVM Overview.json");
	}
}
