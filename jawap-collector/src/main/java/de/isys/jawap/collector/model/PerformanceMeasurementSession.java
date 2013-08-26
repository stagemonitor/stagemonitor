package de.isys.jawap.collector.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PerformanceMeasurementSession {

	private String id;
	private Date startOfSession = new Date();
	private Date endOfSession;
	private List<HttpRequestStats> requests = new ArrayList<HttpRequestStats>();
	private Float cpuUsagePercent;
	private Long garbageCollectionsCount;
	private Long garbageCollectionTime;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getStartOfSession() {
		return startOfSession;
	}

	public void setStartOfSession(Date startOfSession) {
		this.startOfSession = startOfSession;
	}

	public List<HttpRequestStats> getRequests() {
		return requests;
	}

	public void setRequests(List<HttpRequestStats> requests) {
		this.requests = requests;
	}

	public void setCpuUsagePercent(Float cpuUsagePercent) {
		this.cpuUsagePercent = cpuUsagePercent;
	}

	public Float getCpuUsagePercent() {
		return cpuUsagePercent;
	}

	public void setGarbageCollectionsCount(Long garbageCollectionsCount) {
		this.garbageCollectionsCount = garbageCollectionsCount;
	}

	public void setGarbageCollectionTime(Long garbageCollectionTime) {
		this.garbageCollectionTime = garbageCollectionTime;
	}

	public Long getGarbageCollectionsCount() {
		return garbageCollectionsCount;
	}

	public Long getGarbageCollectionTime() {
		return garbageCollectionTime;
	}

	public Date getEndOfSession() {
		return endOfSession;
	}

	public void setEndOfSession(Date endOfSession) {
		this.endOfSession = endOfSession;
	}
}
