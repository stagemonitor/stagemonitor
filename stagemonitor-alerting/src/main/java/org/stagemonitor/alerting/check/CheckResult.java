package org.stagemonitor.alerting.check;

import java.util.Collection;

/**
 * The result of a check
 */
public class CheckResult {

	private final String failingExpression;
	private final Status status;
	private final double currentValue;

	public CheckResult(String failingExpression, double currentValue, Status status) {
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

	public static enum Status {
		OK(0), WARN(1), ERROR(2), CRITICAL(3);

		private final int severity;

		private Status(int severity) {
			this.severity = severity;
		}

		public boolean isMoreSevere(Status other) {
			return severity > other.severity;
		}
	}
}
