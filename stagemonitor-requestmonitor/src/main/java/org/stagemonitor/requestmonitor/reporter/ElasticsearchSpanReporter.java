package org.stagemonitor.requestmonitor.reporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

public class ElasticsearchSpanReporter extends AbstractInterceptedSpanReporter {

	public static final String ES_SPAN_LOGGER = "ElasticsearchSpanReporter";

	private final Logger requestTraceLogger;

	public ElasticsearchSpanReporter() {
		this(LoggerFactory.getLogger(ES_SPAN_LOGGER));
	}

	ElasticsearchSpanReporter(Logger requestTraceLogger) {
		this.requestTraceLogger = requestTraceLogger;
	}

	@Override
	protected void doReport(ReportArguments reportArguments, PostExecutionInterceptorContext context) {
		final String index = "stagemonitor-spans-" + StringUtils.getLogstashStyleDate();
		final String type = "spans";
		if (requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()) {
			requestTraceLogger.info(ElasticsearchClient.getBulkHeader("index", index, type) + JsonUtils.toJson(reportArguments.getSpan()));
		} else {
			if (context.getExcludedProperties().isEmpty()) {
				elasticsearchClient.index(index, type, reportArguments.getSpan());
			} else {
				elasticsearchClient.index(index, type, JsonUtils.toObjectNode(reportArguments.getSpan()).remove(context.getExcludedProperties()));
			}
		}
	}

	@Override
	public boolean isActive(IsActiveArguments isActiveArguments) {
		final boolean logOnly = requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports();
		return (elasticsearchClient.isElasticsearchAvailable() || logOnly) && super.isActive(isActiveArguments);
	}

	/**
	 * Add an {@link PreExecutionRequestTraceReporterInterceptor} to the interceptor list
	 *
	 * @param interceptor the interceptor that should be executed before measurement starts
	 */
	public static void registerPreInterceptor(PreExecutionRequestTraceReporterInterceptor interceptor) {
		final ElasticsearchSpanReporter thiz = getElasticsearchSpanReporter();
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
		final ElasticsearchSpanReporter thiz = getElasticsearchSpanReporter();
		if (thiz != null) {
			thiz.postInterceptors.add(interceptor);
		}
	}

	private static ElasticsearchSpanReporter getElasticsearchSpanReporter() {
		return Stagemonitor
				.getPlugin(RequestMonitorPlugin.class)
				.getRequestMonitor()
				.getReporter(ElasticsearchSpanReporter.class);
	}

}
