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
		metrics.put(name("network_io").tag("ifname", ifname).type("read").unit("bytes").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getRxBytes();
			}
		});
		metrics.put(name("network_io").tag("ifname", ifname).type("read").unit("errors").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getRxErrors();
			}
		});
		metrics.put(name("network_io").tag("ifname", ifname).type("read").unit("dropped").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getRxDropped();
			}
		});
		if (getSnapshot().getRxOverruns() >= 0) {
			metrics.put(name("network_io").tag("ifname", ifname).type("read").unit("overruns").build(), new Gauge<Long>() {
				@Override
				public Long getValue() {
					return getSnapshot().getRxOverruns();
				}
			});
		}
		if (getSnapshot().getRxFrame() >= 0) {
			metrics.put(name("network_io").tag("ifname", ifname).type("read").unit("frame").build(), new Gauge<Long>() {
				@Override
				public Long getValue() {
					return getSnapshot().getRxFrame();
				}
			});
		}

		metrics.put(name("network_io").tag("ifname", ifname).type("write").unit("bytes").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getTxBytes();
			}
		});
		metrics.put(name("network_io").tag("ifname", ifname).type("write").unit("packets").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getTxPackets();
			}
		});
		metrics.put(name("network_io").tag("ifname", ifname).type("write").unit("errors").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getTxErrors();
			}
		});
		metrics.put(name("network_io").tag("ifname", ifname).type("write").unit("dropped").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getTxDropped();
			}
		});

		if (getSnapshot().getTxOverruns() >= 0) {
			metrics.put(name("network_io").tag("ifname", ifname).type("write").unit("overruns").build(), new Gauge<Long>() {
				@Override
				public Long getValue() {
					return getSnapshot().getTxOverruns();
				}
			});
		}
		if (getSnapshot().getTxCarrier() >= 0) {
			metrics.put(name("network_io").tag("ifname", ifname).type("write").unit("carrier").build(), new Gauge<Long>() {
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
