package org.stagemonitor.alerting.incident;

import java.util.List;

import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.core.MeasurementSession;

public class CheckResults {

	private MeasurementSession measurementSession;
	private CheckResult.Status status;
	private List<CheckResult> results;
	private int consecutiveFailures;

	public CheckResults() {
	}

	public CheckResults(MeasurementSession measurementSession, List<CheckResult> results) {
		this(measurementSession, results, 0);
	}

	public CheckResults(CheckResults previousResult, List<CheckResult> results) {
		this(previousResult.getMeasurementSession(), results, previousResult.consecutiveFailures);
	}

	private CheckResults(MeasurementSession measurementSession, List<CheckResult> results, int consecutiveFailures) {
		this.measurementSession = measurementSession;
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

	public MeasurementSession getMeasurementSession() {
		return measurementSession;
	}

	public void setMeasurementSession(MeasurementSession measurementSession) {
		this.measurementSession = measurementSession;
	}
}
