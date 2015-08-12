package org.stagemonitor.os.metrics;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.HashMap;
import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.stagemonitor.core.metrics.metrics2.MetricName;


public class NetworkMetricSet extends AbstractSigarMetricSet<NetInterfaceStat> {

	private final String ifname;

	public NetworkMetricSet(String ifname, Sigar sigar) throws SigarException {
		super(sigar);
		this.ifname = ifname;
	}


	@Override
	public Map<MetricName, Metric> getMetrics() {
		Map<MetricName, Metric> metrics = new HashMap<MetricName, Metric>();
		metrics.put(name("network_read_bytes").tag("ifname", ifname).build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getRxBytes();
			}
		});
		metrics.put(name("network_read_packets").tag("ifname", ifname).build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getRxPackets();
			}
		});
		metrics.put(name("network_read_errors").tag("ifname", ifname).build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getRxErrors();
			}
		});
		metrics.put(name("network_read_dropped").tag("ifname", ifname).build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getRxDropped();
			}
		});
		if (getSnapshot().getRxOverruns() >= 0) {
			metrics.put(name("network_read_overruns").tag("ifname", ifname).build(), new Gauge<Long>() {
				@Override
				public Long getValue() {
					return getSnapshot().getRxOverruns();
				}
			});
		}
		if (getSnapshot().getRxFrame() >= 0) {
			metrics.put(name("network_read_frame").tag("ifname", ifname).build(), new Gauge<Long>() {
				@Override
				public Long getValue() {
					return getSnapshot().getRxFrame();
				}
			});
		}

		metrics.put(name("network_write_bytes").tag("ifname", ifname).build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getTxBytes();
			}
		});
		metrics.put(name("network_write_packets").tag("ifname", ifname).build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getTxPackets();
			}
		});
		metrics.put(name("network_write_errors").tag("ifname", ifname).build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getTxErrors();
			}
		});
		metrics.put(name("network_write_dropped").tag("ifname", ifname).build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getTxDropped();
			}
		});

		if (getSnapshot().getTxOverruns() >= 0) {
			metrics.put(name("network_write_overruns").tag("ifname", ifname).build(), new Gauge<Long>() {
				@Override
				public Long getValue() {
					return getSnapshot().getTxOverruns();
				}
			});
		}
		if (getSnapshot().getTxCarrier() >= 0) {
			metrics.put(name("network_write_carrier").tag("ifname", ifname).build(), new Gauge<Long>() {
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
