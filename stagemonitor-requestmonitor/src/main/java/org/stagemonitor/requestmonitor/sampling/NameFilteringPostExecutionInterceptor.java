package org.stagemonitor.requestmonitor.sampling;

import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

import java.util.Collection;

class NameFilteringPostExecutionInterceptor extends PostExecutionSpanInterceptor {

	private RequestMonitorPlugin requestMonitorPlugin;

	@Override
	public void init(Configuration configuration) {
		requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
	}

	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		final Collection<String> onlyReportRequestsWithName = requestMonitorPlugin
				.getOnlyReportSpansWithName();
		if (StringUtils.isEmpty(context.getSpanContext().getOperationName())) {
			context.shouldNotReport(getClass());
		} else if (!onlyReportRequestsWithName.isEmpty() && !onlyReportRequestsWithName.contains(context.getSpanContext().getOperationName())) {
			context.shouldNotReport(getClass());
		}
	}

}
