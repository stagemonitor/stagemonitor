package de.isys.jawap.collector.jvm;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import de.isys.jawap.collector.core.Configuration;
import de.isys.jawap.collector.core.JawapPlugin;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class JvmPlugin implements JawapPlugin {
	private final Log logger = LogFactory.getLog(getClass());
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
			logger.error(e.getMessage(), e);
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
			logger.error(e.getMessage(), e);
		}
	}
}
