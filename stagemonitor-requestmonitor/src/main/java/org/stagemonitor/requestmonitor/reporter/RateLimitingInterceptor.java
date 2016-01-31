package org.stagemonitor.requestmonitor.reporter;

import java.util.Collection;

import com.codahale.metrics.Meter;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;

public class RateLimitingInterceptor implements ElasticsearchRequestTraceReporterInterceptor {

	private RequestMonitorPlugin requestMonitorPlugin;

	@Override
	public void init(Configuration configuration) {
		requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
	}
	@Override
	public boolean interceptReport(RequestTrace requestTrace, Meter reportingRate, Collection<String> excludedProperties) {
		final int maxReportingRate = requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch();
		if (Integer.MAX_VALUE == maxReportingRate) {
			return true;
		} else if (maxReportingRate <= 0) {
			return false;
		}
		return 60 * reportingRate.getOneMinuteRate() <= maxReportingRate;
	}

}
