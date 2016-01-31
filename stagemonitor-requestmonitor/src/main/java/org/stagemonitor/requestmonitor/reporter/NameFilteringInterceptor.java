package org.stagemonitor.requestmonitor.reporter;

import java.util.Collection;

import com.codahale.metrics.Meter;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;

public class NameFilteringInterceptor implements ElasticsearchRequestTraceReporterInterceptor {

	private RequestMonitorPlugin requestMonitorPlugin;

	@Override
	public void init(Configuration configuration) {
		requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
	}
	@Override
	public boolean interceptReport(RequestTrace requestTrace, Meter reportingRate, Collection<String> excludedProperties) {
		final Collection<String> onlyReportRequestsWithName = requestMonitorPlugin.getOnlyReportRequestsWithNameToElasticsearch();
		return onlyReportRequestsWithName.isEmpty() || onlyReportRequestsWithName.contains(requestTrace.getName());
	}

}
