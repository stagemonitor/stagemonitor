package org.stagemonitor.requestmonitor.sampling;

import org.stagemonitor.core.StagemonitorSPI;
import org.stagemonitor.requestmonitor.reporter.ElasticsearchSpanReporter;

/**
 * Allows implementers omit reporting a {@link io.opentracing.Span}
 * <p/>
 * The pre interceptor is executed before the monitoring of the current request starts.
 * The big difference to {@link PostExecutionSpanReporterInterceptor} is that when a request is excluded from
 * reporting by a pre interceptor, the call tree won't be collected for that request.
 * <p/>
 * To add an interceptor, call {@link ElasticsearchSpanReporter#registerPreInterceptor(PreExecutionSpanReporterInterceptor)}
 * or place a file under <code>META-INF/services/org.stagemonitor.requestmonitor.sampling.PreExecutionSpanReporterInterceptor</code>
 * and insert the canonical class name of your implementation.
 */
public abstract class PreExecutionSpanReporterInterceptor implements StagemonitorSPI {

	/**
	 * This method is called before a span gets reported.
	 * <p/>
	 * The implementer of this method can decide whether or not to report the span or to exclude certain properties.
	 *
	 * @param context contextual information about the current report that is about to happen
	 */
	public abstract void interceptReport(PreExecutionInterceptorContext context);
}
