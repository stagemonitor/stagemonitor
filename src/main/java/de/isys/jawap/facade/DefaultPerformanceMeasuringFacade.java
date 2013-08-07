package de.isys.jawap.facade;

import de.isys.jawap.model.HttpRequestStats;
import de.isys.jawap.model.PerformanceMeasurementSession;
import de.isys.jawap.model.PeriodicPerformanceData;
import de.isys.jawap.service.PerformanceMeasuringService;
import org.webstage.shop.core.configuration.DBProperties;

public class DefaultPerformanceMeasuringFacade implements PerformanceMeasuringFacade {

	private PerformanceMeasuringService performanceMeasuringService;

	@Override
	public void save(PerformanceMeasurementSession performanceMeasurementSession) {
		performanceMeasuringService.save(performanceMeasurementSession);
	}

	@Override
	public void save(HttpRequestStats requestStats) {
		if (!DBProperties.PERFORMANCE_STATS_LOG_ONLY.getBoolean()
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
