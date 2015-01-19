package org.stagemonitor.alerting.incident;

import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.check.CheckGroup;
import org.stagemonitor.core.MeasurementSession;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Incident {

	private int version;
	private Date firstFailAt;
	private Date resolvedAt;
	private Check.Status oldStatus;
	private Check.Status newStatus;
	private String checkGroupId;
	private String checkGroupName;
	private Map<String, CheckResultsByInstance> resultsByHostAndInstance = new HashMap<String, CheckResultsByInstance>();

	public Incident() {
	}

	public Incident(CheckGroup checkGroup, MeasurementSession measurementSession, List<Check.Result> checkResults) {
		this.newStatus = Check.Result.getMostSevereStatus(checkResults);
		this.checkGroupId = checkGroup.getId();
		this.checkGroupName = checkGroup.getName();
		this.firstFailAt = new Date();

		setCheckResults(measurementSession, checkResults);
	}

	public Incident(Incident previousIncident, MeasurementSession measurementSession, List<Check.Result> checkResults) {
		version = previousIncident.version + 1;
		oldStatus = previousIncident.newStatus;
		checkGroupId = previousIncident.checkGroupId;
		checkGroupName = previousIncident.checkGroupName;
		resultsByHostAndInstance = previousIncident.resultsByHostAndInstance;
		firstFailAt = previousIncident.getFirstFailAt();

		setCheckResults(measurementSession, checkResults);
	}

	private void setCheckResults(MeasurementSession measurementSession, List<Check.Result> checkResults) {
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
		if (newStatus == Check.Status.OK) {
			resolvedAt = new Date();
		}
	}

	private boolean isRemoveHost(List<Check.Result> checkResults, String instance, CheckResultsByInstance checkResultsByInstance) {
		return checkResults.isEmpty() && checkResultsByInstance != null && checkResultsByInstance.isOnlyInstance(instance);
	}

	public Check.Status getMostSevereStatus() {
		Check.Status mostSevereStatus = Check.Status.OK;
		for (CheckResultsByInstance checkResultByInstance : resultsByHostAndInstance.values()) {
			final Check.Status mostSevereStatusOfInstance = checkResultByInstance.getMostSevereStatus();
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

	public Check.Status getOldStatus() {
		return oldStatus;
	}

	public void setOldStatus(Check.Status oldStatus) {
		this.oldStatus = oldStatus;
	}

	public Check.Status getNewStatus() {
		return newStatus;
	}

	public void setNewStatus(Check.Status newStatus) {
		this.newStatus = newStatus;
	}

	/**
	 * The id of the corresponding {@link CheckGroup}.
	 * It is also used as the primary key for the incident.
	 *
	 * @return the id of the corresponding {@link CheckGroup}.
	 */
	public String getCheckGroupId() {
		return checkGroupId;
	}

	public void setCheckGroupId(String checkGroupId) {
		this.checkGroupId = checkGroupId;
	}

	public String getCheckGroupName() {
		return checkGroupName;
	}

	public void setCheckGroupName(String checkGroupName) {
		this.checkGroupName = checkGroupName;
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
		if (checkGroupId != null ? !checkGroupId.equals(incident.checkGroupId) : incident.checkGroupId != null)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = version;
		result = 31 * result + (checkGroupId != null ? checkGroupId.hashCode() : 0);
		return result;
	}

	public Incident getIncidentWithPreviousVersion() {
		final Incident incident = new Incident();
		incident.setCheckGroupId(checkGroupId);
		incident.setVersion(version - 1);
		return incident;
	}

	@Override
	public String toString() {
		String s = "Incident for check group '" + checkGroupName + "':\n" +
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
				for (Check.Result result : instanceEntry.getValue().getResults()) {
					s += host + '|' + instance + '|' + result.getStatus() + '|' + result.getFailingExpression() +
							'|' + result.getCurrentValue()+ "\n";
				}
			}
		}
		return s;
	}
}
