package de.isys.jawap.collector.facade;

import de.isys.jawap.collector.model.HttpRequestStats;
import de.isys.jawap.collector.model.PerformanceMeasurementSession;
import de.isys.jawap.collector.model.PeriodicPerformanceData;

public interface PerformanceMeasuringFacade {

	void save(PerformanceMeasurementSession performanceMeasurementSession);

	void save(HttpRequestStats requestStats);

	void update(PerformanceMeasurementSession performanceMeasurementSession);

	void save(PeriodicPerformanceData periodicPerformanceData);

}