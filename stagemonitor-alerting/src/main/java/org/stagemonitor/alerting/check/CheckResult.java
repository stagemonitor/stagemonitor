package org.stagemonitor.alerting.check;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

/**
 * The result of a check
 */
public class CheckResult {

	private final String failingExpression;
	private final Status status;
	private final double currentValue;

	@JsonCreator
	public CheckResult(@JsonProperty("failingExpression") String failingExpression,
					   @JsonProperty("currentValue") double currentValue,
					   @JsonProperty("status")Status status) {

		this.failingExpression = failingExpression;
		this.currentValue = currentValue;
		this.status = status;
	}

	public static Status getMostSevereStatus(Collection<CheckResult> results) {
		Status mostSevereStatus = Status.OK;
		for (CheckResult result : results) {
			if (result.getStatus().isMoreSevere(mostSevereStatus)) {
				mostSevereStatus = result.getStatus();
			}
		}
		return mostSevereStatus;
	}

	public String getFailingExpression() {
		return failingExpression;
	}

	public Status getStatus() {
		return status;
	}

	public double getCurrentValue() {
		return currentValue;
	}

	public enum Status {
		CRITICAL(3), ERROR(2), WARN(1), OK(0);

		private final int severity;

		Status(int severity) {
			this.severity = severity;
		}

		public boolean isMoreSevere(Status other) {
			return severity > other.severity;
		}
	}
}
