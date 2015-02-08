package org.stagemonitor.alerting.incident;

import org.stagemonitor.alerting.check.CheckResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckResultsByInstance {

	private Map<String, ResultOnSpecificHostAndInstance> resultsByInstance = new HashMap<String, ResultOnSpecificHostAndInstance>();

	public CheckResultsByInstance() {
	}

	public CheckResultsByInstance(String instanceName, List<CheckResult> checkResults) {
		resultsByInstance.put(instanceName, new ResultOnSpecificHostAndInstance(checkResults));
	}

	public void addCheckResults(String instance, List<CheckResult> results) {
		if (results.isEmpty()) {
			resultsByInstance.remove(instance);
		} else {
			resultsByInstance.put(instance, new ResultOnSpecificHostAndInstance(resultsByInstance.get(instance), results));
		}
	}

	public boolean isOnlyInstance(String instance) {
		return resultsByInstance.size() == 1 && resultsByInstance.containsKey(instance);
	}

	public CheckResult.Status getMostSevereStatus() {
		CheckResult.Status mostSevereStatus = CheckResult.Status.OK;
		for (ResultOnSpecificHostAndInstance resultOnSpecificHostAndInstance : resultsByInstance.values()) {
			if (resultOnSpecificHostAndInstance.getStatus().isMoreSevere(mostSevereStatus)) {
				mostSevereStatus = resultOnSpecificHostAndInstance.getStatus();
			}
		}
		return mostSevereStatus;
	}

	public Map<String, ResultOnSpecificHostAndInstance> getResultsByInstance() {
		return resultsByInstance;
	}

	public ResultOnSpecificHostAndInstance getResultsByInstance(String instance) {
		return resultsByInstance.get(instance);
	}

	public void setResultsByInstance(Map<String, ResultOnSpecificHostAndInstance> resultsByInstance) {
		this.resultsByInstance = resultsByInstance;
	}

	public int getConsecutiveFailures() {
		int consecutiveFailures = 0;
		for (ResultOnSpecificHostAndInstance resultOnSpecificHostAndInstance : resultsByInstance.values()) {
			consecutiveFailures = Math.max(consecutiveFailures, resultOnSpecificHostAndInstance.getConsecutiveFailures());
		}
		return consecutiveFailures;
	}
}
