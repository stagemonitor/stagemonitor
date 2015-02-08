package org.stagemonitor.alerting.incident;

import org.stagemonitor.alerting.check.CheckResult;

import java.util.List;

public class ResultOnSpecificHostAndInstance {
	private CheckResult.Status status;
	private List<CheckResult> results;
	private int consecutiveFailures;

	public ResultOnSpecificHostAndInstance() {
	}

	public ResultOnSpecificHostAndInstance(List<CheckResult> results) {
		this(results, 0);
	}

	public ResultOnSpecificHostAndInstance(ResultOnSpecificHostAndInstance previousResult, List<CheckResult> results) {
		this(results, previousResult.consecutiveFailures);
	}

	private ResultOnSpecificHostAndInstance(List<CheckResult> results, int consecutiveFailures) {
		this.results = results;
		this.status = CheckResult.getMostSevereStatus(results);
		this.consecutiveFailures = consecutiveFailures;

		if (status != CheckResult.Status.OK) {
			this.consecutiveFailures++;
		}
	}

	public CheckResult.Status getStatus() {
		return status;
	}

	public void setStatus(CheckResult.Status status) {
		this.status = status;
	}

	public List<CheckResult> getResults() {
		return results;
	}

	public void setResults(List<CheckResult> results) {
		this.results = results;
	}

	public int getConsecutiveFailures() {
		return consecutiveFailures;
	}

	public void setConsecutiveFailures(int consecutiveFailures) {
		this.consecutiveFailures = consecutiveFailures;
	}
}
