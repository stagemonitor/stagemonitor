package org.stagemonitor.core.metrics.annotations;

import com.codahale.metrics.annotation.Gauge;
import org.stagemonitor.core.metrics.MonitorGauges;

@MonitorGauges
public class GaugeTestObject {

	public GaugeTestObject() {
		this(1);
	}

	public GaugeTestObject(int i) {
	}

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
