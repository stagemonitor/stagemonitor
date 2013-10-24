package de.isys.jawap.collector.facade;

import de.isys.jawap.collector.Configuration;
import de.isys.jawap.collector.model.HttpRequestContext;
import de.isys.jawap.collector.model.PerformanceMeasurementSession;
import de.isys.jawap.collector.model.PeriodicPerformanceData;
import de.isys.jawap.collector.service.PerformanceMeasuringService;

public class DefaultPerformanceMeasuringFacade implements PerformanceMeasuringFacade {

	private PerformanceMeasuringService performanceMeasuringService;

	@Override
	public void save(PerformanceMeasurementSession performanceMeasurementSession) {
		performanceMeasuringService.save(performanceMeasurementSession);
	}

	@Override
	public void save(HttpRequestContext requestStats) {
		if (!Configuration.PERFORMANCE_STATS_LOG_ONLY
				&& requestStats.getPerformanceMeasurementSession().getId() != null) {
			performanceMeasuringService.save(requestStats);
		}
		performanceMeasuringService.logStats(requestStats);
	}

	@Override
	public void update(PerformanceMeasurementSession performanceMeasurementSession) {
		performanceMeasuringService.update(performanceMeasurementSession);
	}

	@Override
	public void save(PeriodicPerformanceData periodicPerformanceData) {
		performanceMeasuringService.save(periodicPerformanceData);
	}
}
