package org.stagemonitor.alerting;

import java.util.Date;
import java.util.List;

public class Incident {

	private List<Check.Result> checkResults;
	private Date time = new Date();
	private Check.Status oldStatus;
	private Check.Status newStatus;
	private String checkGroupId;
	private String checkGroupName;
	private int consecutiveFailures;
	// TODO application host instance

	public Incident() {
	}

	public Incident(CheckGroup checkGroup, Check.Status oldStatus, Check.Status newStatus, List<Check.Result> checkResults) {
		this.checkResults = checkResults;
		this.oldStatus = oldStatus;
		this.newStatus = newStatus;
		this.checkGroupId = checkGroup.getId();
		this.checkGroupName = checkGroup.getName();
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
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

	public List<Check.Result> getCheckResults() {
		return checkResults;
	}

	public void setCheckResults(List<Check.Result> checkResults) {
		this.checkResults = checkResults;
	}

	public int getConsecutiveFailures() {
		return consecutiveFailures;
	}

	public void setConsecutiveFailures(int consecutiveFailures) {
		this.consecutiveFailures = consecutiveFailures;
	}

	public void incrementConsecutiveFailures() {
		this.consecutiveFailures++;
	}

	public boolean hasStageChange() {
		return oldStatus != newStatus;
	}
}
