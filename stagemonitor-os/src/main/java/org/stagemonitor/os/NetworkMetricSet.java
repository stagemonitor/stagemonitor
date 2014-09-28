package org.stagemonitor.os;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.NetStat;
import org.hyperic.sigar.Tcp;
import org.stagemonitor.core.util.GraphiteSanitizer;

import java.util.HashMap;
import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;

public class NetworkMetricSet implements MetricSet {

	private final String baseName;
	private final NetInterfaceStat netInterfaceStat;

	public NetworkMetricSet(String ifname, NetInterfaceStat netInterfaceStat) {
		this.baseName = name("os.net", GraphiteSanitizer.sanitizeGraphiteMetricSegment(ifname));
		this.netInterfaceStat = netInterfaceStat;
	}

	@Override
	public Map<String, Metric> getMetrics() {
		Map<String, Metric> metrics = new HashMap<String, Metric>();
		metrics.put(name(baseName, "read.bytes"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return netInterfaceStat.getRxBytes();
			}
		});
		metrics.put(name(baseName, "read.packets"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return netInterfaceStat.getRxPackets();
			}
		});
		metrics.put(name(baseName, "read.errors"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return netInterfaceStat.getRxErrors();
			}
		});
		metrics.put(name(baseName, "read.dropped"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return netInterfaceStat.getRxDropped();
			}
		});
		metrics.put(name(baseName, "read.overruns"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return netInterfaceStat.getRxOverruns();
			}
		});
		metrics.put(name(baseName, "read.frame"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return netInterfaceStat.getRxFrame();
			}
		});

		metrics.put(name(baseName, "write.bytes"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return netInterfaceStat.getTxBytes();
			}
		});
		metrics.put(name(baseName, "write.packets"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return netInterfaceStat.getTxPackets();
			}
		});
		metrics.put(name(baseName, "write.errors"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return netInterfaceStat.getTxErrors();
			}
		});
		metrics.put(name(baseName, "write.dropped"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return netInterfaceStat.getTxDropped();
			}
		});
		metrics.put(name(baseName, "write.overruns"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return netInterfaceStat.getTxOverruns();
			}
		});
		metrics.put(name(baseName, "write.carrier"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return netInterfaceStat.getTxCarrier();
			}
		});
		return metrics;
	}
}
