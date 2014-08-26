package org.stagemonitor.core.metrics.aspects;

import com.codahale.metrics.annotation.Gauge;
import org.stagemonitor.core.metrics.MonitorGauges;

@MonitorGauges
public class GaugeTestObject {

	@Gauge
	private int gaugeDefault() {
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
