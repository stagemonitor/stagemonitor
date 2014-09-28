package org.stagemonitor.os;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Sigar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;

public class CpuMetricSet implements MetricSet {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final CpuPerc cpuPerc;
	private final CpuInfo cpuInfo;

	public CpuMetricSet(CpuPerc cpuPerc, CpuInfo cpuInfo) {
		this.cpuPerc = cpuPerc;
		this.cpuInfo = cpuInfo;
	}

	@Override
	public Map<String, Metric> getMetrics() {
		Map<String, Metric> metrics = new HashMap<String, Metric>();
		metrics.put("os.cpu.usage.sys", new Gauge<Double>() {
			@Override
			public Double getValue() {
				return cpuPerc.getSys();
			}
		});
		metrics.put("os.cpu.usage.user", new Gauge<Double>() {
			@Override
			public Double getValue() {
				return cpuPerc.getUser();
			}
		});
		metrics.put("os.cpu.usage.idle", new Gauge<Double>() {
			@Override
			public Double getValue() {
				return cpuPerc.getIdle();
			}
		});

		metrics.put("os.cpu.info.mhz", new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return cpuInfo.getMhz();
			}
		});
		metrics.put("os.cpu.info.cores", new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return cpuInfo.getTotalCores();
			}
		});
		if (cpuInfo.getCacheSize() != Sigar.FIELD_NOTIMPL) {
			metrics.put("os.cpu.info.cache", new Gauge<Long>() {
				@Override
				public Long getValue() {
					return cpuInfo.getCacheSize();
				}
			});
		}

		try {
			final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.newPlatformMXBeanProxy(
					ManagementFactory.getPlatformMBeanServer(), ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
					OperatingSystemMXBean.class);
			if (operatingSystemMXBean.getSystemLoadAverage() >= 0) {
				metrics.put("os.cpu.queueLength", new Gauge<Double>() {
					@Override
					public Double getValue() {
						return Math.max(0.0, operatingSystemMXBean.getSystemLoadAverage() - operatingSystemMXBean.getAvailableProcessors());
					}
				});
				metrics.put("os.cpu.load.1m", new Gauge<Double>() {
					@Override
					public Double getValue() {
						return operatingSystemMXBean.getSystemLoadAverage();
					}
				});
			}
		} catch (IOException e) {
			logger.warn(e.getMessage() + " (this exception is ignored)", e);
		}
		return metrics;
	}

}
