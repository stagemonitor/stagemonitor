package org.stagemonitor.core;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MeasurementSession {

	private String applicationName;
	private String hostName;
	private String instanceName;

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	@JsonIgnore
	public boolean isInitialized() {
		return applicationName != null && instanceName != null && hostName != null;
	}

	@Override
	public String toString() {
		return "MeasurementSession{" +
				"applicationName='" + applicationName + '\'' +
				", hostName='" + hostName + '\'' +
				", instanceName='" + instanceName + '\'' +
				'}';
	}

}
