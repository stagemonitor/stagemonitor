package org.stagemonitor.collector.core.metrics.aspects;

import com.codahale.metrics.annotation.Gauge;

@MonitorGauges
public class GaugeTestObject {

	@Gauge
	public int gaugeDefault() {
		return 1;
	}
	@Gauge
	public static int staticGaugeDefault() {
		return 1;
	}

	@Gauge(absolute = true)
	public void gaugeAbsolute() {
	}

	@Gauge(name = "myGaugeName")
	public void gaugeName() {
	}

	@Gauge(name = "myGaugeNameAbsolute", absolute = true)
	public void gaugeNameAbsolute() {
	}
}
