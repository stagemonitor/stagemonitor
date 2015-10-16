package org.stagemonitor.os.metrics;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.HashMap;
import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.stagemonitor.core.metrics.metrics2.MetricName;

public class FileSystemMetricSet extends AbstractSigarMetricSet<FileSystemUsage> {

	private String mountpoint;

	public FileSystemMetricSet(String mountpoint, Sigar sigar) throws SigarException {
		super(sigar);
		this.mountpoint = mountpoint;
	}

	@Override
	public Map<MetricName, Metric> getMetrics() {
		Map<MetricName, Metric> metrics = new HashMap<MetricName, Metric>();
		metrics.put(name("disk_usage").tag("mountpoint", mountpoint).type("total").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getTotal() * 1024;
			}
		});
		metrics.put(name("disk_usage").tag("mountpoint", mountpoint).type("free").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getFree() * 1024;
			}
		});
		metrics.put(name("disk_usage").tag("mountpoint", mountpoint).type("used").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getUsed() * 1024;
			}
		});
		metrics.put(name("disk_usage_percent").tag("mountpoint", mountpoint).build(), new Gauge<Double>() {
			@Override
			public Double getValue() {
				return getSnapshot().getUsePercent() * 100.0;
			}
		});
		metrics.put(name("disk_io").tag("mountpoint", mountpoint).type("read").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getDiskReadBytes();
			}
		});
		metrics.put(name("disk_io").tag("mountpoint", mountpoint).type("write").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getDiskWriteBytes();
			}
		});
		metrics.put(name("disk_queue").tag("mountpoint", mountpoint).build(), new Gauge<Double>() {
			@Override
			public Double getValue() {
				return getSnapshot().getDiskQueue();
			}
		});
		if (getSnapshot().getDiskServiceTime() >= 0) {
			metrics.put(name("disk_service_time").tag("mountpoint", mountpoint).build(), new Gauge<Double>() {
				@Override
				public Double getValue() {
					return getSnapshot().getDiskServiceTime();
				}
			});
		}
		return metrics;
	}

	@Override
	FileSystemUsage loadSnapshot(Sigar sigar) throws SigarException {
		return sigar.getFileSystemUsage(mountpoint);
	}
}
