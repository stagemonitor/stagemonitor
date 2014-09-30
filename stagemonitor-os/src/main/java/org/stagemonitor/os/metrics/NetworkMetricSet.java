package org.stagemonitor.os.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.stagemonitor.core.util.GraphiteSanitizer;

import java.util.HashMap;
import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;

public class NetworkMetricSet extends AbstractSigarMetricSet<NetInterfaceStat> {

	private final String baseName;
	private final String ifname;

	public NetworkMetricSet(String ifname, Sigar sigar) throws SigarException {
		super(sigar);
		this.ifname = ifname;
		this.baseName = name("os.net", GraphiteSanitizer.sanitizeGraphiteMetricSegment(ifname));
	}


	@Override
	public Map<String, Metric> getMetrics() {
		Map<String, Metric> metrics = new HashMap<String, Metric>();
		metrics.put(name(baseName, "read.bytes"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getRxBytes();
			}
		});
		metrics.put(name(baseName, "read.packets"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getRxPackets();
			}
		});
		metrics.put(name(baseName, "read.errors"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getRxErrors();
			}
		});
		metrics.put(name(baseName, "read.dropped"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getRxDropped();
			}
		});
		if (getSnapshot().getRxOverruns() >= 0) {
			metrics.put(name(baseName, "read.overruns"), new Gauge<Long>() {
				@Override
				public Long getValue() {
					return getSnapshot().getRxOverruns();
				}
			});
		}
		if (getSnapshot().getRxFrame() >= 0) {
			metrics.put(name(baseName, "read.frame"), new Gauge<Long>() {
				@Override
				public Long getValue() {
					return getSnapshot().getRxFrame();
				}
			});
		}

		metrics.put(name(baseName, "write.bytes"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getTxBytes();
			}
		});
		metrics.put(name(baseName, "write.packets"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getTxPackets();
			}
		});
		metrics.put(name(baseName, "write.errors"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getTxErrors();
			}
		});
		metrics.put(name(baseName, "write.dropped"), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getTxDropped();
			}
		});

		if (getSnapshot().getTxOverruns() >= 0) {
			metrics.put(name(baseName, "write.overruns"), new Gauge<Long>() {
				@Override
				public Long getValue() {
					return getSnapshot().getTxOverruns();
				}
			});
		}
		if (getSnapshot().getTxCarrier() >= 0) {
			metrics.put(name(baseName, "write.carrier"), new Gauge<Long>() {
				@Override
				public Long getValue() {
					return getSnapshot().getTxCarrier();
				}
			});
		}
		return metrics;
	}

	@Override
	NetInterfaceStat loadSnapshot(Sigar sigar) throws SigarException {
		return sigar.getNetInterfaceStat(ifname);
	}
}
