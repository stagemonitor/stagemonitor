package org.stagemonitor.alerting;

import org.stagemonitor.core.util.StringUtils;

import java.util.Date;

public class Incident {

	private String time;
	private Check.Status oldStatus;
	private Check.Status newStatus;
	private double currentValue;
	private String checkId;
	private String description;

	public Incident(Check.Status oldStatus, Check.Status newStatus, double currentValue, String checkId, String description) {
		this.time = StringUtils.dateAsIsoString(new Date());
		this.oldStatus = oldStatus;
		this.newStatus = newStatus;
		this.currentValue = currentValue;
	}

	public String getTime() {
		return time;
	}

	public Check.Status getOldStatus() {
		return oldStatus;
	}

	public Check.Status getNewStatus() {
		return newStatus;
	}

	public double getCurrentValue() {
		return currentValue;
	}
}
