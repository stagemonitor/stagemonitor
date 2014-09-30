package org.stagemonitor.os.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.stagemonitor.core.util.GraphiteSanitizer;

import java.util.HashMap;
import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;

public class FileSystemMetricSet extends AbstractSigarMetricSet<FileSystemUsage> {

	private final String baseName;
	private String fileSystemName;

	public FileSystemMetricSet(String fileSystemName, Sigar sigar) throws SigarException {
		super(sigar);
		this.fileSystemName = fileSystemName;
		this.baseName = name("os.fs", GraphiteSanitizer.sanitizeGraphiteMetricSegment(fileSystemName.replace("\\", "")));
	}

	@Override
	public Map<String, Metric> getMetrics() {
		Map<String, Metric> metrics = new HashMap<String, Metric>();
		metrics.put(name(baseName, "usage.total"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getTotal() * 1024;
			}
		});
		metrics.put(name(baseName, "usage.free"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getFree()* 1024;
			}
		});
		metrics.put(name(baseName, "usage.used"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getUsed()* 1024;
			}
		});
		metrics.put(name(baseName, "usage-percent"), new Gauge<Double>() {
			@Override
			public Double getValue() {
				return getSnapshot().getUsePercent();
			}
		});
		metrics.put(name(baseName, "reads.bytes"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getDiskReadBytes();
			}
		});
		metrics.put(name(baseName, "writes.bytes"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getDiskWriteBytes();
			}
		});
		metrics.put(name(baseName, "disk.queue"), new Gauge<Double>() {
			@Override
			public Double getValue() {
				return getSnapshot().getDiskQueue();
			}
		});
		if (getSnapshot().getDiskServiceTime() >= 0) {
			metrics.put(name(baseName, "disk.serviceTime"), new Gauge<Double>() {
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
		return sigar.getFileSystemUsage(fileSystemName);
	}
}
