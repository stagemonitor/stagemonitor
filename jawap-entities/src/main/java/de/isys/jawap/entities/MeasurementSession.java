package de.isys.jawap.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MeasurementSession {

	@Id
	@GeneratedValue
	private Integer id;
	private Date startOfSession = new Date();
	private Date endOfSession;
	private String applicationName;
	private String hostName;
	private String instanceName;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Date getStartOfSession() {
		return startOfSession;
	}

	public void setStartOfSession(Date startOfSession) {
		this.startOfSession = startOfSession;
	}

	public Date getEndOfSession() {
		return endOfSession;
	}

	public void setEndOfSession(Date endOfSession) {
		this.endOfSession = endOfSession;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = avoidCommonSpecialCharacters(applicationName);
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = avoidCommonSpecialCharacters(hostName);
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = avoidCommonSpecialCharacters(instanceName);
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

	/**
	 * Actually, special characters are no real problem, because everything will be url encoded before sending to
	 * graphite. But that leads to difficult to read names in the UI.
	 *
	 * @param s the string to clean
	 * @return the input string without common special characters
	 */
	private String avoidCommonSpecialCharacters(String s) {
		if (s == null) return null;
		return s.replace(' ', '-').replace('.', '-');
	}
}
