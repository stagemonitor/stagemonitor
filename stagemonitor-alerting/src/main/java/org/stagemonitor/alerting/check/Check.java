package org.stagemonitor.alerting.check;

import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class Check {

	private String metric;
	private Threshold warn;
	private Threshold error;
	private Threshold critical;

	public Check() {
	}

	public Check(String metric, Threshold warn, Threshold error, Threshold critical) {
		this.metric = metric;
		this.warn = warn;
		this.error = error;
		this.critical = critical;
	}

	public Result check(String target, double actualValue) {
		if (critical != null && critical.isExceeded(actualValue)) {
			return new Result(getCheckExpressionAsString(target, critical), actualValue, Status.CRITICAL);
		}
		if (error != null && error.isExceeded(actualValue)) {
			return new Result(getCheckExpressionAsString(target, error), actualValue, Status.ERROR);
		}
		if (warn != null && warn.isExceeded(actualValue)) {
			return new Result(getCheckExpressionAsString(target, warn), actualValue, Status.WARN);
		}
		return new Result(null, actualValue, Status.OK);
	}

	private String getCheckExpressionAsString(String target, Threshold threshold) {
		return target + "." + metric + " " + threshold.toString();
	}

	public String getMetric() {
		return metric;
	}

	public void setMetric(String metric) {
		this.metric = metric;
	}

	public Threshold getWarn() {
		return warn;
	}

	public Threshold getError() {
		return error;
	}

	public Threshold getCritical() {
		return critical;
	}

	public void setWarn(Threshold warn) {
		this.warn = warn;
	}

	public void setError(Threshold error) {
		this.error = error;
	}

	public void setCritical(Threshold critical) {
		this.critical = critical;
	}

	/**
	 * The result of a check
	 */
	public static class Result {

		private final String failingExpression;
		private final Status status;
		private final double currentValue;

		public Result(String failingExpression, double currentValue, Status status) {
			this.failingExpression = failingExpression;
			this.currentValue = currentValue;
			this.status = status;
		}

		public static List<Result> getResultsWithMostSevereStatus(List<Check.Result> results) {
			Check.Status mostSevereStatus = getMostSevereStatus(results);
			List<Check.Result> resultsWithStatus = new LinkedList<Result>();
			for (Check.Result result : results) {
				if (result.getStatus() == mostSevereStatus) {
					resultsWithStatus.add(result);
				}
			}
			return resultsWithStatus;
		}

		public static Check.Status getMostSevereStatus(List<Check.Result> results) {
			Check.Status mostSevereStatus = Check.Status.OK;
			for (Check.Result result : results) {
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
