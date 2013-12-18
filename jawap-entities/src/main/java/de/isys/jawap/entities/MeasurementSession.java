package de.isys.jawap.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;

@Entity
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
