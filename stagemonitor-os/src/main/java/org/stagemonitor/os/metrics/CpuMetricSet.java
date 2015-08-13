package org.stagemonitor.os.metrics;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.metrics.metrics2.MetricName;

public class CpuMetricSet extends AbstractSigarMetricSet<CpuPerc> {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final CpuInfo cpuInfo;

	public CpuMetricSet(Sigar sigar, CpuInfo cpuInfo) throws SigarException {
		super(sigar);
		this.cpuInfo = cpuInfo;
	}

	@Override
	CpuPerc loadSnapshot(Sigar sigar) throws SigarException {
		return sigar.getCpuPerc();
	}

	@Override
	public Map<MetricName, Metric> getMetrics() {
		Map<MetricName, Metric> metrics = new HashMap<MetricName, Metric>();
		metrics.put(name("cpu_usage").type("sys").build(), new Gauge<Double>() {
			@Override
			public Double getValue() {
				return getSnapshot().getSys();
			}
		});
		metrics.put(name("cpu_usage").type("user").build(), new Gauge<Double>() {
			@Override
			public Double getValue() {
				return getSnapshot().getUser();
			}
		});
		metrics.put(name("cpu_usage").type("idle").build(), new Gauge<Double>() {
			@Override
			public Double getValue() {
				return getSnapshot().getIdle();
			}
		});
		metrics.put(name("cpu_usage").type("nice").build(), new Gauge<Double>() {
			@Override
			public Double getValue() {
				return getSnapshot().getNice();
			}
		});
		metrics.put(name("cpu_usage").type("wait").build(), new Gauge<Double>() {
			@Override
			public Double getValue() {
				return getSnapshot().getWait();
			}
		});
		metrics.put(name("cpu_usage").type("interrupt").build(), new Gauge<Double>() {
			@Override
			public Double getValue() {
				return getSnapshot().getIrq();
			}
		});
		metrics.put(name("cpu_usage").type("soft-interrupt").build(), new Gauge<Double>() {
			@Override
			public Double getValue() {
				return getSnapshot().getSoftIrq();
			}
		});
		metrics.put(name("cpu_usage").type("stolen").build(), new Gauge<Double>() {
			@Override
			public Double getValue() {
				return getSnapshot().getStolen();
			}
		});
		metrics.put(name("cpu_usage_percent").build(), new Gauge<Double>() {
			@Override
			public Double getValue() {
				return getSnapshot().getCombined();
			}
		});

		metrics.put(name("cpu_info_mhz").build(), new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return cpuInfo.getMhz();
			}
		});
		metrics.put(name("cpu_info_cores").build(), new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return cpuInfo.getTotalCores();
			}
		});
		if (cpuInfo.getCacheSize() != Sigar.FIELD_NOTIMPL) {
			metrics.put(name("cpu_info_cache").build(), new Gauge<Long>() {
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
				metrics.put(name("cpu_queueLength").build(), new Gauge<Double>() {
					@Override
					public Double getValue() {
						return Math.max(0.0, operatingSystemMXBean.getSystemLoadAverage() - operatingSystemMXBean.getAvailableProcessors());
					}
				});
				metrics.put(name("cpu_load").tag("timeframe", "1m").build(), new Gauge<Double>() {
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
