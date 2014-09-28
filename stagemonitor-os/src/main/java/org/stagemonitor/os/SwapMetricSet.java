package org.stagemonitor.os;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import org.hyperic.sigar.Swap;

import java.util.HashMap;
import java.util.Map;

public class SwapMetricSet implements MetricSet {

	private final Swap swap;

	public SwapMetricSet(Swap swap) {
		this.swap = swap;
	}

	@Override
	public Map<String, Metric> getMetrics() {
		Map<String, Metric> metrics = new HashMap<String, Metric>();
		metrics.put("os.swap.usage.free", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return swap.getFree();
			}
		});
		metrics.put("os.swap.usage.used", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return swap.getUsed();
			}
		});
		metrics.put("os.swap.usage.total", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return swap.getTotal();
			}
		});

		metrics.put("os.swap.page.in", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return swap.getPageIn();
			}
		});
		metrics.put("os.swap.page.out", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return swap.getPageOut();
			}
		});
		return metrics;
	}
}
