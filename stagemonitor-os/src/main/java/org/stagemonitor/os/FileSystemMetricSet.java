package org.stagemonitor.os;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.stagemonitor.core.util.GraphiteSanitizer;

import java.util.HashMap;
import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;

public class FileSystemMetricSet implements MetricSet {

	private final String baseName;
	private final FileSystemUsage fileSystem;

	public FileSystemMetricSet(String fileSystemName, FileSystemUsage fileSystem) {
		this.baseName = name("os.fs", GraphiteSanitizer.sanitizeGraphiteMetricSegment(fileSystemName));
		this.fileSystem = fileSystem;
	}

	@Override
	public Map<String, Metric> getMetrics() {
		Map<String, Metric> metrics = new HashMap<String, Metric>();
		metrics.put(name(baseName, "usage.total"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return fileSystem.getTotal();
			}
		});
		metrics.put(name(baseName, "usage.free"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return fileSystem.getFree();
			}
		});
		metrics.put(name(baseName, "usage.used"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return fileSystem.getUsed();
			}
		});
		metrics.put(name(baseName, "reads.bytes"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return fileSystem.getDiskReadBytes();
			}
		});
		metrics.put(name(baseName, "writes.bytes"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return fileSystem.getDiskWriteBytes();
			}
		});
		metrics.put(name(baseName, "disk.queue"), new Gauge<Double>() {
			@Override
			public Double getValue() {
				return fileSystem.getDiskQueue();
			}
		});
		if (fileSystem.getDiskServiceTime() >= 0) {
			metrics.put(name(baseName, "disk.serviceTime"), new Gauge<Double>() {
				@Override
				public Double getValue() {
					return fileSystem.getDiskServiceTime();
				}
			});
		}
		return metrics;
	}
}
