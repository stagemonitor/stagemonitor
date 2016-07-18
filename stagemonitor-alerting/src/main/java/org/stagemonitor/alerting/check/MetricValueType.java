package org.stagemonitor.alerting.check;

public enum MetricValueType {

	COUNT("count"),
	MEAN("mean"),
	MIN("min"),
	MAX("max"),
	STDDEV("stddev"),
	P50("p50"),
	P75("p75"),
	P95("p95"),
	P98("p98"),
	P99("p99"),
	P999("p999"),
	MEAN_RATE("mean_rate"),
	M1_RATE("m1_rate"),
	M5_RATE("m5_rate"),
	M15_RATE("m15_rate"),
	VALUE("value");

	private final String name;

	MetricValueType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
