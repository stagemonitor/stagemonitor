package org.stagemonitor.alerting.alerter;

import org.stagemonitor.alerting.check.Check;

public class Subscription {

	private String alerterType;
	boolean alertOnBackToOk, alertOnWarn, alertOnError, alertOnCritical;

	public String getAlerterType() {
		return alerterType;
	}

	public void setAlerterType(String alerterType) {
		this.alerterType = alerterType;
	}

	public boolean isAlertOnBackToOk() {
		return alertOnBackToOk;
	}

	public void setAlertOnBackToOk(boolean alertOnBackToOk) {
		this.alertOnBackToOk = alertOnBackToOk;
	}

	public boolean isAlertOnWarn() {
		return alertOnWarn;
	}

	public void setAlertOnWarn(boolean alertOnWarn) {
		this.alertOnWarn = alertOnWarn;
	}

	public boolean isAlertOnError() {
		return alertOnError;
	}

	public void setAlertOnError(boolean alertOnError) {
		this.alertOnError = alertOnError;
	}

	public boolean isAlertOnCritical() {
		return alertOnCritical;
	}

	public void setAlertOnCritical(boolean alertOnCritical) {
		this.alertOnCritical = alertOnCritical;
	}

	public boolean isAlertOn(Check.Status status) {
		switch (status) {
			case OK:
				return alertOnBackToOk;
			case WARN:
				return alertOnWarn;
			case ERROR:
				return alertOnError;
			case CRITICAL:
				return alertOnCritical;
			default:
				return false;
		}
	}
}
