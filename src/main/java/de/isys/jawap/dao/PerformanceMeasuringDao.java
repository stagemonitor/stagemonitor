package de.isys.jawap.dao;

import de.isys.jawap.model.HttpRequestStats;
import de.isys.jawap.model.PerformanceMeasurementSession;
import de.isys.jawap.model.PeriodicPerformanceData;

public interface PerformanceMeasuringDao {
	void save(PerformanceMeasurementSession performanceMeasurementSession);

	void save(HttpRequestStats requestStats);

	void update(PerformanceMeasurementSession performanceMeasurementSession);

	void save(PeriodicPerformanceData periodicPerformanceData);
}
