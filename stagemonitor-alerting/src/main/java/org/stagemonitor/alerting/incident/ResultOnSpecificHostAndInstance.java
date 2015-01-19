package org.stagemonitor.alerting.incident;

import org.stagemonitor.alerting.check.Check;

import java.util.List;

public class ResultOnSpecificHostAndInstance {
	private Check.Status status;
	private List<Check.Result> results;
	private int consecutiveFailures;

	public ResultOnSpecificHostAndInstance() {
	}

	public ResultOnSpecificHostAndInstance(List<Check.Result> results) {
		this(results, 0);
	}

	public ResultOnSpecificHostAndInstance(ResultOnSpecificHostAndInstance previousResult, List<Check.Result> results) {
		this(results, previousResult.consecutiveFailures);
	}

	private ResultOnSpecificHostAndInstance(List<Check.Result> results, int consecutiveFailures) {
		this.results = results;
		this.status = Check.Result.getMostSevereStatus(results);
		this.consecutiveFailures = consecutiveFailures;

		if (status != Check.Status.OK) {
			this.consecutiveFailures++;
		}
	}

	public Check.Status getStatus() {
		return status;
	}

	public void setStatus(Check.Status status) {
		this.status = status;
	}

	public List<Check.Result> getResults() {
		return results;
	}

	public void setResults(List<Check.Result> results) {
		this.results = results;
	}

	public int getConsecutiveFailures() {
		return consecutiveFailures;
	}

	public void setConsecutiveFailures(int consecutiveFailures) {
		this.consecutiveFailures = consecutiveFailures;
	}
}
