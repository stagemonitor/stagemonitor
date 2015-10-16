package org.stagemonitor.os.metrics;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.HashMap;
import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.RatioGauge;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Swap;
import org.stagemonitor.core.metrics.metrics2.MetricName;

public class SwapMetricSet extends AbstractSigarMetricSet<Swap> {

	public SwapMetricSet(Sigar sigar) throws SigarException{
		super(sigar);
	}

	@Override
	Swap loadSnapshot(Sigar sigar) throws SigarException {
		return sigar.getSwap();
	}

	@Override
	public Map<MetricName, Metric> getMetrics() {
		Map<MetricName, Metric> metrics = new HashMap<MetricName, Metric>();
		metrics.put(name("swap_usage").type("free").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getFree();
			}
		});
		metrics.put(name("swap_usage").type("used").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getUsed();
			}
		});
		metrics.put(name("swap_usage").type("total").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getTotal();
			}
		});
		metrics.put(name("swap_usage_percent").build(), new RatioGauge() {
			@Override
			protected Ratio getRatio() {
				return Ratio.of(getSnapshot().getUsed(), getSnapshot().getTotal() * 100.0);
			}
		});

		metrics.put(name("swap_pages").type("in").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getPageIn();
			}
		});
		metrics.put(name("swap_pages").type("out").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getPageOut();
			}
		});
		return metrics;
	}
}
