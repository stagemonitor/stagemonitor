package org.stagemonitor.web.reporter;

import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.requestmonitor.reporter.PostExecutionInterceptorContext;
import org.stagemonitor.requestmonitor.reporter.PostExecutionRequestTraceReporterInterceptor;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.HttpRequestTrace;

/**
 * This class makes sure that requests which contain the do not track header (dnt) are not monitored
 *
 * You can customize this behaviour with {@link WebPlugin#honorDoNotTrackHeader}
 */
public class DoNotTrackPostExecutionInterceptor extends PostExecutionRequestTraceReporterInterceptor {

	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		if (context.getRequestTrace() == null) {
			// TODO
			return;
		}
		if (!context.getConfig(WebPlugin.class).isHonorDoNotTrackHeader()) {
			return;
		}
		final RequestTrace requestTrace = context.getRequestTrace();
		if (requestTrace instanceof HttpRequestTrace) {
			final HttpRequestTrace httpRequestTrace = (HttpRequestTrace) requestTrace;
			if ("1".equals(httpRequestTrace.getHeaders().get("dnt"))) {
				context.shouldNotReport(getClass());
			}
		}
	}
}
