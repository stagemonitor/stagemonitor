package org.stagemonitor.core;


import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static org.stagemonitor.core.util.StringUtils.replaceWhitespacesWithDash;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.stagemonitor.core.util.StringUtils;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = NONE)
public class MeasurementSession {

	private final String id;
	private final String applicationName;
	private final String hostName;
	private final String instanceName;
	private final long startTimestamp;
	private long endTimestamp;
	@JsonIgnore
	private final String stringRepresentation;

	public MeasurementSession(
			String applicationName,
			String hostName,
			String instanceName) {
		this(UUID.randomUUID().toString(), applicationName, hostName, instanceName);
	}

	@JsonCreator
	public MeasurementSession(@JsonProperty("id") String id,
							  @JsonProperty("applicationName") String applicationName,
							  @JsonProperty("hostName") String hostName,
							  @JsonProperty("instanceName") String instanceName) {

		this.applicationName = replaceWhitespacesWithDash(applicationName);
		this.hostName = replaceWhitespacesWithDash(hostName);
		this.instanceName = replaceWhitespacesWithDash(instanceName);
		this.id = id;
		stringRepresentation = "[application=" + applicationName + "] [instance=" + instanceName + "] [host=" + hostName + "]";
		startTimestamp = System.currentTimeMillis();
	}

	public String getId() {
		return id;
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

	@JsonProperty("@timestamp")
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
		result.put("measurement_start", Long.toString(startTimestamp));
		result.put("application", applicationName);
		result.put("host", hostName);
		result.put("instance", instanceName);
		return Collections.unmodifiableMap(result);
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}

}
