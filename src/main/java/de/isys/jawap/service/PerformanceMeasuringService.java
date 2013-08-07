package de.isys.jawap.service;

import de.isys.jawap.model.HttpRequestStats;
import de.isys.jawap.model.PerformanceMeasurementSession;
import de.isys.jawap.model.PeriodicPerformanceData;

public interface PerformanceMeasuringService {
	void save(PerformanceMeasurementSession performanceMeasurementSession);

	void save(HttpRequestStats requestStats);

	void logStats(HttpRequestStats requestStats);

	void update(PerformanceMeasurementSession performanceMeasurementSession);

	void save(PeriodicPerformanceData periodicPerformanceData);
}
