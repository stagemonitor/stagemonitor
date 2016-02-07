package org.stagemonitor.requestmonitor.reporter;

/**
 * Allows implementers to customize or omit reporting a request trace to Elasticsearch
 * <p/>
 * To add an interceptor, call {@link ElasticsearchRequestTraceReporter#registerInterceptor(ElasticsearchRequestTraceReporterInterceptor)}
 * or place a file under <code>META-INF/services/org.stagemonitor.requestmonitor.reporter.ElasticsearchRequestTraceReporterInterceptor</code>
 * and insert the canonical class name of your implementation.
 */
public interface ElasticsearchRequestTraceReporterInterceptor {

	/**
	 * This method is called before a request trace gets reported to Elasticsearch.
	 * <p/>
	 * The implementer of this method can decide whether or not to report the request trace or to exclude certain properties.
	 *
	 * @param context contextual information about the current report that is about to happen
	 */
	void interceptReport(InterceptContext context);
}
