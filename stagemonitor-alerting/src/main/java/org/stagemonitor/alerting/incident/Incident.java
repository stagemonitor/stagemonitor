package org.stagemonitor.alerting.incident;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.core.MeasurementSession;

public class Incident {

	private int version;
	private Date firstFailAt;
	private Date resolvedAt;
	private CheckResult.Status oldStatus;
	private CheckResult.Status newStatus;
	private String checkId;
	private String checkName;
	private Map<String, CheckResultsByInstance> resultsByHostAndInstance = new HashMap<String, CheckResultsByInstance>();

	public Incident() {
	}

	public Incident(Check check, MeasurementSession measurementSession, List<CheckResult> checkResults) {
		this.newStatus = CheckResult.getMostSevereStatus(checkResults);
		this.checkId = check.getId();
		this.checkName = check.getName();
		this.firstFailAt = new Date();

		setCheckResults(measurementSession, checkResults);
	}

	public Incident(Incident previousIncident, MeasurementSession measurementSession, List<CheckResult> checkResults) {
		version = previousIncident.version + 1;
		oldStatus = previousIncident.newStatus;
		checkId = previousIncident.checkId;
		checkName = previousIncident.checkName;
		resultsByHostAndInstance = previousIncident.resultsByHostAndInstance;
		firstFailAt = previousIncident.getFirstFailAt();

		setCheckResults(measurementSession, checkResults);
	}

	private void setCheckResults(MeasurementSession measurementSession, List<CheckResult> checkResults) {
		final String host = measurementSession.getHostName();
		final String instance = measurementSession.getInstanceName();
		final CheckResultsByInstance checkResultsByInstance = resultsByHostAndInstance.get(host);
		if (isRemoveHost(checkResults, instance, checkResultsByInstance)) {
			resultsByHostAndInstance.remove(host);
		} else if (checkResultsByInstance != null) {
			checkResultsByInstance.addCheckResults(instance, checkResults);
		} else {
			resultsByHostAndInstance.put(host, new CheckResultsByInstance(instance, checkResults));
		}
		newStatus = getMostSevereStatus();
		if (newStatus == CheckResult.Status.OK) {
			resolvedAt = new Date();
		}
	}

	private boolean isRemoveHost(List<CheckResult> checkResults, String instance, CheckResultsByInstance checkResultsByInstance) {
		return checkResults.isEmpty() && checkResultsByInstance != null && checkResultsByInstance.isOnlyInstance(instance);
	}

	public CheckResult.Status getMostSevereStatus() {
		CheckResult.Status mostSevereStatus = CheckResult.Status.OK;
		for (CheckResultsByInstance checkResultByInstance : resultsByHostAndInstance.values()) {
			final CheckResult.Status mostSevereStatusOfInstance = checkResultByInstance.getMostSevereStatus();
			if (mostSevereStatusOfInstance.isMoreSevere(mostSevereStatus)) {
				mostSevereStatus = mostSevereStatusOfInstance;
			}
		}
		return mostSevereStatus;
	}

	public int getConsecutiveFailures() {
		int consecutiveFailures = 0;
		for (CheckResultsByInstance checkResultByInstance : resultsByHostAndInstance.values()) {
			consecutiveFailures = Math.max(consecutiveFailures, checkResultByInstance.getConsecutiveFailures());
		}
		return consecutiveFailures;
	}

	/**
	 * The version is used for optimistic concurrency control
	 *
	 * @return the version
	 */
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public Date getFirstFailAt() {
		return firstFailAt;
	}

	public void setFirstFailAt(Date firstFailAt) {
		this.firstFailAt = firstFailAt;
	}

	public CheckResult.Status getOldStatus() {
		return oldStatus;
	}

	public void setOldStatus(CheckResult.Status oldStatus) {
		this.oldStatus = oldStatus;
	}

	public CheckResult.Status getNewStatus() {
		return newStatus;
	}

	public void setNewStatus(CheckResult.Status newStatus) {
		this.newStatus = newStatus;
	}

	/**
	 * The id of the corresponding {@link org.stagemonitor.alerting.check.Check}.
	 * It is also used as the primary key for the incident.
	 *
	 * @return the id of the corresponding {@link org.stagemonitor.alerting.check.Check}.
	 */
	public String getCheckId() {
		return checkId;
	}

	public void setCheckId(String checkId) {
		this.checkId = checkId;
	}

	public String getCheckName() {
		return checkName;
	}

	public void setCheckName(String checkName) {
		this.checkName = checkName;
	}

	public void incrementVersion() {
		this.version++;
	}

	public boolean hasStageChange() {
		return oldStatus != newStatus;
	}

	public Map<String, CheckResultsByInstance> getResultsByHostAndInstance() {
		return resultsByHostAndInstance;
	}

	public void setResultsByHostAndInstance(Map<String, CheckResultsByInstance> resultsByHostAndInstance) {
		this.resultsByHostAndInstance = resultsByHostAndInstance;
	}

	public Date getResolvedAt() {
		return resolvedAt;
	}

	public void setResolvedAt(Date resolvedAt) {
		this.resolvedAt = resolvedAt;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Incident incident = (Incident) o;

		if (version != incident.version) return false;
		if (checkId != null ? !checkId.equals(incident.checkId) : incident.checkId != null)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = version;
		result = 31 * result + (checkId != null ? checkId.hashCode() : 0);
		return result;
	}

	public Incident getIncidentWithPreviousVersion() {
		final Incident incident = new Incident();
		incident.setCheckId(checkId);
		incident.setVersion(version - 1);
		return incident;
	}

	@Override
	public String toString() {
		String s = "Incident for check group '" + checkName + "':\n" +
				"firstFailAt=" + firstFailAt + "\n" +
				"resolvedAt=" + resolvedAt + "\n" +
				"oldStatus=" + oldStatus + "\n" +
				"newStatus=" + newStatus + "\n" +
				"host|instance|status|description|current value\n" +
				"----|--------|------|-----------|-------------\n";
		for (Map.Entry<String, CheckResultsByInstance> hostEntry : resultsByHostAndInstance.entrySet()) {
			final String host = hostEntry.getKey();
			for (Map.Entry<String, ResultOnSpecificHostAndInstance> instanceEntry : hostEntry.getValue().getResultsByInstance().entrySet()) {
				final String instance = instanceEntry.getKey();
				for (CheckResult result : instanceEntry.getValue().getResults()) {
					s += host + '|' + instance + '|' + result.getStatus() + '|' + result.getFailingExpression() +
							'|' + result.getCurrentValue()+ "\n";
				}
			}
		}
		return s;
	}
}
