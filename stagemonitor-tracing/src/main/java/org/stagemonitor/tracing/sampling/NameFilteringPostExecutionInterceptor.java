package org.stagemonitor.tracing.sampling;

import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.util.StringUtils;

import java.util.Collection;

public class NameFilteringPostExecutionInterceptor extends PostExecutionSpanInterceptor {

	private TracingPlugin tracingPlugin;

	@Override
	public void init(ConfigurationRegistry configuration) {
		tracingPlugin = configuration.getConfig(TracingPlugin.class);
	}

	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		final Collection<String> onlyReportRequestsWithName = tracingPlugin
				.getOnlyReportSpansWithName();
		if (StringUtils.isEmpty(context.getSpanContext().getOperationName())) {
			context.shouldNotReport(getClass());
		} else if (!onlyReportRequestsWithName.isEmpty() && !onlyReportRequestsWithName.contains(context.getSpanContext().getOperationName())) {
			context.shouldNotReport(getClass());
		}
	}

}
