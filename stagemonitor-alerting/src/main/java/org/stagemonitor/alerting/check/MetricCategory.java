package org.stagemonitor.alerting.check;

public enum MetricCategory {

	TIMER("timer"), HISTOGRAM("histograms"), COUNTER("counters"), GAUGE("gauges"), METER("meters");

	private final String path;

	private MetricCategory(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}
}
