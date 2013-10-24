package de.isys.jawap.collector.facade;

import de.isys.jawap.collector.model.HttpRequestContext;
import de.isys.jawap.collector.model.PerformanceMeasurementSession;
import de.isys.jawap.collector.model.PeriodicPerformanceData;

public interface PerformanceMeasuringFacade {

	void save(PerformanceMeasurementSession performanceMeasurementSession);

	void save(HttpRequestContext requestStats);

	void update(PerformanceMeasurementSession performanceMeasurementSession);

	void save(PeriodicPerformanceData periodicPerformanceData);

}