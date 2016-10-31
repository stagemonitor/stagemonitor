package org.stagemonitor.web.reporter;

import org.stagemonitor.requestmonitor.reporter.PostExecutionInterceptorContext;
import org.stagemonitor.requestmonitor.reporter.PostExecutionRequestTraceReporterInterceptor;
import org.stagemonitor.requestmonitor.utils.SpanTags;
import org.stagemonitor.web.WebPlugin;

/**
 * This class makes sure that requests which contain the do not track header (dnt) are not monitored
 *
 * You can customize this behaviour with {@link WebPlugin#honorDoNotTrackHeader}
 */
public class DoNotTrackPostExecutionInterceptor extends PostExecutionRequestTraceReporterInterceptor {

	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		if (!context.getConfig(WebPlugin.class).isHonorDoNotTrackHeader()) {
			return;
		}
		if ("1".equals(context.getInternalSpan().getTags().get(SpanTags.HTTP_HEADERS_PREFIX + "dnt"))) {
			context.shouldNotReport(getClass());
		}
	}
}
