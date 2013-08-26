package de.isys.jawap.collector.service;

import de.isys.jawap.collector.model.HttpRequestStats;
import de.isys.jawap.collector.model.PerformanceMeasurementSession;
import de.isys.jawap.collector.model.PeriodicPerformanceData;

public interface PerformanceMeasuringService {
	void save(PerformanceMeasurementSession performanceMeasurementSession);

	void save(HttpRequestStats requestStats);

	void logStats(HttpRequestStats requestStats);

	void update(PerformanceMeasurementSession performanceMeasurementSession);

	void save(PeriodicPerformanceData periodicPerformanceData);
}
