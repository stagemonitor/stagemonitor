package org.stagemonitor.alerting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.stagemonitor.core.util.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 */
public class Check {

	private final String id;
	private final Pattern metricName;
	private final String metric;
	private List<Threshold> warn = new LinkedList<Threshold>();
	private List<Threshold> error = new LinkedList<Threshold>();
	private List<Threshold> critical = new LinkedList<Threshold>();

	@JsonCreator
	public Check(@JsonProperty("id") String id, @JsonProperty("metricName") Pattern metricName, @JsonProperty("metric") String metric) {
		this.id = StringUtils.slugify(id);
		this.metricName = metricName;
		this.metric = metric;
	}

	public Status check(double actualValue) {
		if (Threshold.isAllExceeded(critical, actualValue)) {
			return Status.CRITICAL;
		}
		if (Threshold.isAllExceeded(error, actualValue)) {
			return Status.ERROR;
		}
		if (Threshold.isAllExceeded(warn, actualValue)) {
			return Status.WARN;
		}
		return Status.OK;
	}

	public String getId() {
		return id;
	}

	public Pattern getMetricName() {
		return metricName;
	}

	public String getMetric() {
		return metric;
	}

	public List<Threshold> getWarn() {
		return warn;
	}

	public List<Threshold> getError() {
		return error;
	}

	public List<Threshold> getCritical() {
		return critical;
	}

	public void setWarn(List<Threshold> warn) {
		this.warn = warn;
	}

	public void setError(List<Threshold> error) {
		this.error = error;
	}

	public void setCritical(List<Threshold> critical) {
		this.critical = critical;
	}

	public static enum Status {
		OK, WARN, ERROR, CRITICAL
	}
}
