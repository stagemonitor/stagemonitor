package org.stagemonitor.core;


import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.net.InetAddress;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.stagemonitor.core.util.StringUtils;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = NONE)
public class MeasurementSession {

	private final String applicationName;
	private final String hostName;
	private final String instanceName;
	private final long startTimestamp;
	private long endTimestamp;
	@JsonIgnore
	private final String stringRepresentation;

	@JsonCreator
	public MeasurementSession(@JsonProperty("applicationName") String applicationName,
							  @JsonProperty("hostName") String hostName,
							  @JsonProperty("instanceName") String instanceName) {

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

	@JsonIgnore
	public boolean isInitialized() {
		return applicationName != null && instanceName != null && hostName != null;
	}

	public String getStart() {
		return StringUtils.timestampAsIsoString(startTimestamp);
	}

	public long getStartTimestamp() {
		return startTimestamp;
	}

	public String getEnd() {
		if (endTimestamp > 0) {
			return StringUtils.timestampAsIsoString(endTimestamp);
		}
		return null;
	}

	public Long getEndTimestamp() {
		if (endTimestamp > 0) {
			return endTimestamp;
		}
		return null;
	}

	public void setEndTimestamp(long endTimestamp) {
		this.endTimestamp = endTimestamp;
	}

	@JsonIgnore
	public boolean isNull() {
		return applicationName == null && instanceName == null && hostName == null;
	}

	@JsonIgnore
	public Map<String, String> asMap() {
		final TreeMap<String, String> result = new TreeMap<String, String>();
		result.put("application", applicationName);
		result.put("host", hostName);
		result.put("instance", instanceName);
		return result;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MeasurementSession)) return false;

		MeasurementSession that = (MeasurementSession) o;

		if (applicationName != null ? !applicationName.equals(that.applicationName) : that.applicationName != null)
			return false;
		if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;
		if (instanceName != null ? !instanceName.equals(that.instanceName) : that.instanceName != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = applicationName != null ? applicationName.hashCode() : 0;
		result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
		result = 31 * result + (instanceName != null ? instanceName.hashCode() : 0);
		return result;
	}
}
