package org.stagemonitor.requestmonitor.reporter;

/**
 * Allows implementers to customize or omit reporting a request trace to Elasticsearch
 * <p/>
 * The post interceptor is executed after the complete request trace has been collected and right before it should be
 * reported to elasticsearch.
 * <p/>
 * To add an interceptor, call {@link ElasticsearchRequestTraceReporter#registerPostInterceptor(PostExecutionRequestTraceReporterInterceptor)}
 * or place a file under <code>META-INF/services/org.stagemonitor.requestmonitor.reporter.PostExecutionRequestTraceReporterInterceptor</code>
 * and insert the canonical class name of your implementation.
 */
public abstract class PostExecutionRequestTraceReporterInterceptor {

	/**
	 * This method is called before a request trace gets reported to Elasticsearch.
	 * <p/>
	 * The implementer of this method can decide whether or not to report the request trace or to exclude certain properties.
	 *
	 * @param context contextual information about the current report that is about to happen
	 */
	public abstract void interceptReport(PostExecutionInterceptorContext context);
}
