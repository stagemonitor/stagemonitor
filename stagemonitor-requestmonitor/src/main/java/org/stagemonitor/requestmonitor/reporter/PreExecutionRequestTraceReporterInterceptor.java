package org.stagemonitor.requestmonitor.reporter;

/**
 * Allows implementers omit reporting a request trace to Elasticsearch
 * <p/>
 * The pre interceptor is executed before the monitoring of the current request starts.
 * The big difference to {@link PostExecutionRequestTraceReporterInterceptor} is that when a request is excluded from
 * reporting by a pre interceptor, the call tree won't be collected for that request.
 * <p/>
 * To add an interceptor, call {@link ElasticsearchRequestTraceReporter#registerPreInterceptor(PreExecutionRequestTraceReporterInterceptor)}
 * or place a file under <code>META-INF/services/org.stagemonitor.requestmonitor.reporter.PreExecutionRequestTraceReporterInterceptor</code>
 * and insert the canonical class name of your implementation.
 */
public abstract class PreExecutionRequestTraceReporterInterceptor {

	/**
	 * This method is called before a request trace gets reported to Elasticsearch.
	 * <p/>
	 * The implementer of this method can decide whether or not to report the request trace or to exclude certain properties.
	 *
	 * @param context contextual information about the current report that is about to happen
	 */
	public abstract void interceptReport(PreExecutionInterceptorContext context);
}
