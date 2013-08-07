package de.isys.jawap.facade;

import de.isys.jawap.model.HttpRequestStats;
import de.isys.jawap.model.PerformanceMeasurementSession;
import de.isys.jawap.model.PeriodicPerformanceData;

public interface PerformanceMeasuringFacade {

	void save(PerformanceMeasurementSession performanceMeasurementSession);

	void save(HttpRequestStats requestStats);

	void update(PerformanceMeasurementSession performanceMeasurementSession);

	void save(PeriodicPerformanceData periodicPerformanceData);

}