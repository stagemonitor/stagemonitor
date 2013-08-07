package de.isys.jawap.model;

public class PeriodicPerformanceData {

	private String id;
	private final PerformanceMeasurementSession performanceMeasurementSession;
	private long totalMemory;
	private long freeMemory;
	private float cpuUsagePercent;
	private ThreadPoolMetrics appServerThreadPoolMetrics;
	private ThreadPoolMetrics springThreadPoolMetrics;
	private ThreadPoolMetrics springScheduledThreadPoolMetrics;

	public PeriodicPerformanceData(PerformanceMeasurementSession performanceMeasurementSession) {
		this.performanceMeasurementSession = performanceMeasurementSession;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getTotalMemory() {
		return totalMemory;
	}

	public void setTotalMemory(long totalMemory) {
		this.totalMemory = totalMemory;
	}

	public long getFreeMemory() {
		return freeMemory;
	}

	public void setFreeMemory(long freeMemory) {
		this.freeMemory = freeMemory;
	}

	public float getCpuUsagePercent() {
		return cpuUsagePercent;
	}

	public void setCpuUsagePercent(float cpuUsagePercent) {
		this.cpuUsagePercent = cpuUsagePercent;
	}

	public PerformanceMeasurementSession getPerformanceMeasurementSession() {
		return performanceMeasurementSession;
	}

	public ThreadPoolMetrics getAppServerThreadPoolMetrics() {
		return appServerThreadPoolMetrics;
	}

	public void setAppServerThreadPoolMetrics(ThreadPoolMetrics appServerThreadPoolMetrics) {
		this.appServerThreadPoolMetrics = appServerThreadPoolMetrics;
	}

	public void setSpringThreadPoolMetrics(ThreadPoolMetrics springThreadPoolMetrics) {
		this.springThreadPoolMetrics = springThreadPoolMetrics;
	}

	public void setSpringScheduledThreadPoolMetrics(ThreadPoolMetrics springScheduledThreadPoolMetrics) {
		this.springScheduledThreadPoolMetrics = springScheduledThreadPoolMetrics;
	}

	public ThreadPoolMetrics getSpringThreadPoolMetrics() {
		return springThreadPoolMetrics;
	}

	public ThreadPoolMetrics getSpringScheduledThreadPoolMetrics() {
		return springScheduledThreadPoolMetrics;
	}
}
