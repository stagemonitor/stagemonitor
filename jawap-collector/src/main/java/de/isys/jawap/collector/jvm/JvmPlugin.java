package de.isys.jawap.collector.jvm;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import de.isys.jawap.collector.core.JawapPlugin;

public class JvmPlugin implements JawapPlugin {
	@Override
	public void initializePlugin() {
		MetricRegistry registry = SharedMetricRegistries.getOrCreate("jvm");
		registry.registerAll(new GarbageCollectorMetricSet());
		registry.registerAll(new MemoryUsageGaugeSet());

		final CpuUtilisationWatch cpuWatch = new CpuUtilisationWatch();
		cpuWatch.start();
		registry.register("CPU_utilisation", new Gauge<Float>() {
			@Override
			public Float getValue() {
				try {
					return cpuWatch.getCpuUsagePercent();
				} finally {
					cpuWatch.start();
				}
			}
		});
	}
}
