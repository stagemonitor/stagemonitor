package org.stagemonitor.os.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.RatioGauge;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Swap;

import java.util.HashMap;
import java.util.Map;

public class SwapMetricSet extends AbstractSigarMetricSet<Swap> {

	public SwapMetricSet(Sigar sigar) throws SigarException{
		super(sigar);
	}

	@Override
	Swap loadSnapshot(Sigar sigar) throws SigarException {
		return sigar.getSwap();
	}

	@Override
	public Map<String, Metric> getMetrics() {
		Map<String, Metric> metrics = new HashMap<String, Metric>();
		metrics.put("os.swap.usage.free", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getFree();
			}
		});
		metrics.put("os.swap.usage.used", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getUsed();
			}
		});
		metrics.put("os.swap.usage.total", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getTotal();
			}
		});
		metrics.put("os.swap.usage-percent", new RatioGauge() {
			@Override
			protected Ratio getRatio() {
				return Ratio.of(getSnapshot().getUsed(), getSnapshot().getTotal());
			}
		});

		metrics.put("os.swap.page.in", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getPageIn();
			}
		});
		metrics.put("os.swap.page.out", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getPageOut();
			}
		});
		return metrics;
	}
}
