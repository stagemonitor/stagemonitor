package org.stagemonitor.core;


import java.net.InetAddress;

public class MeasurementSession {

	private final String applicationName;
	private final String hostName;
	private final String instanceName;
	private final long startTimestamp;
	private long endTimestamp;

	private final String stringRepresentation;

	public MeasurementSession(String applicationName, String hostName, String instanceName) {
		this.applicationName = applicationName;
		this.hostName = hostName;
		this.instanceName = instanceName;
		stringRepresentation = "[application=" + applicationName + "] [instance=" + instanceName + "] [host=" + hostName + "]";
		startTimestamp = System.currentTimeMillis();
	}

	public String getApplicationName() {
		return applicationName;
	}

	public String getHostName() {
		return hostName;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public boolean isInitialized() {
		return applicationName != null && instanceName != null && hostName != null;
	}

	public long getStartTimestamp() {
		return startTimestamp;
	}

	public long getEndTimestamp() {
		return endTimestamp;
	}

	public void setEndTimestamp(long endTimestamp) {
		this.endTimestamp = endTimestamp;
	}

	public boolean isNull() {
		return applicationName == null && instanceName == null && hostName == null;
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}

	public static String getNameOfLocalHost() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			return getHostNameFromEnv();
		}
	}

	static String getHostNameFromEnv() {
		// try environment properties.
		String host = System.getenv("COMPUTERNAME");
		if (host != null) {
			return host;
		}
		host = System.getenv("HOSTNAME");
		if (host != null) {
			return host;
		}
		return null;
	}
}
