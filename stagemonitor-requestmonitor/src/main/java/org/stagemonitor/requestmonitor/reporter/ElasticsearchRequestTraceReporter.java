package org.stagemonitor.requestmonitor.reporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;

/**
 * An implementation of {@link RequestTraceReporter} that sends the {@link RequestTrace} to Elasticsearch
 */
public class ElasticsearchRequestTraceReporter extends AbstractInterceptedRequestTraceReporter {

	public static final String ES_REQUEST_TRACE_LOGGER = "ElasticsearchRequestTraces";

	private final Logger requestTraceLogger;

	public ElasticsearchRequestTraceReporter() {
		this(LoggerFactory.getLogger(ES_REQUEST_TRACE_LOGGER));
	}

	ElasticsearchRequestTraceReporter(Logger requestTraceLogger) {
		this.requestTraceLogger = requestTraceLogger;
	}

	@Override
	protected <T extends RequestTrace> void doReport(T requestTrace, PostExecutionInterceptorContext context) {
		final String index = "stagemonitor-requests-" + StringUtils.getLogstashStyleDate();
		final String type = "requests";
		if (true) return;
		if (!requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()) {
			if (context.getExcludedProperties().isEmpty()) {
				elasticsearchClient.index(index, type, requestTrace);
			} else {
				elasticsearchClient
						.index(index, type, JsonUtils.toObjectNode(requestTrace).remove(context.getExcludedProperties()));
			}
		} else {
			requestTraceLogger.info(ElasticsearchClient.getBulkHeader("index", index, type) + JsonUtils.toJson(requestTrace));
		}
	}

	@Override
	public boolean isActive(IsActiveArguments isActiveArguments) {
		final boolean urlAvailable = !corePlugin.getElasticsearchUrls().isEmpty();
		final boolean logOnly = requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports();
		return (urlAvailable || logOnly) && super.isActive(isActiveArguments);
	}

	/**
	 * Add an {@link PreExecutionRequestTraceReporterInterceptor} to the interceptor list
	 *
	 * @param interceptor the interceptor that should be executed before measurement starts
	 */
	public static void registerPreInterceptor(PreExecutionRequestTraceReporterInterceptor interceptor) {
		final ElasticsearchRequestTraceReporter thiz = getElasticsearchRequestTraceReporter();
		if (thiz != null) {
			thiz.preInterceptors.add(interceptor);
		}
	}
	/**
	 * Add an {@link PostExecutionRequestTraceReporterInterceptor} to the interceptor list
	 *
	 * @param interceptor the interceptor that should be executed before each report
	 */
	public static void registerPostInterceptor(PostExecutionRequestTraceReporterInterceptor interceptor) {
		final ElasticsearchRequestTraceReporter thiz = getElasticsearchRequestTraceReporter();
		if (thiz != null) {
			thiz.postInterceptors.add(interceptor);
		}
	}

	private static ElasticsearchRequestTraceReporter getElasticsearchRequestTraceReporter() {
		return Stagemonitor
					.getPlugin(RequestMonitorPlugin.class)
					.getRequestMonitor()
					.getReporter(ElasticsearchRequestTraceReporter.class);
	}

}
