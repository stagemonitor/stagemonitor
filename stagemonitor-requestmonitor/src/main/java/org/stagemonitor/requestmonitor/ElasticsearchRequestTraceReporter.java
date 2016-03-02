package org.stagemonitor.requestmonitor;

import java.util.Collection;

import com.codahale.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;

/**
 * An implementation of {@link RequestTraceReporter} that sends the {@link RequestTrace} to Elasticsearch
 */
public class ElasticsearchRequestTraceReporter implements RequestTraceReporter {

	public static final String ES_REQUEST_TRACE_LOGGER = "ElasticsearchRequestTraces";

	private final CorePlugin corePlugin;
	private final RequestMonitorPlugin requestMonitorPlugin;
	private final ElasticsearchClient elasticsearchClient;
	private final Meter reportingRate = new Meter();
	private final Logger requestTraceLogger;

	public ElasticsearchRequestTraceReporter(CorePlugin corePlugin, RequestMonitorPlugin requestMonitorPlugin) {
		this(corePlugin, requestMonitorPlugin, LoggerFactory.getLogger(ES_REQUEST_TRACE_LOGGER));
	}

	public ElasticsearchRequestTraceReporter(CorePlugin corePlugin, RequestMonitorPlugin requestMonitorPlugin, Logger requestTraceLogger) {
		this.corePlugin = corePlugin;
		this.requestMonitorPlugin = requestMonitorPlugin;
		this.elasticsearchClient = corePlugin.getElasticsearchClient();
		this.requestTraceLogger = requestTraceLogger;
	}

	@Override
	public <T extends RequestTrace> void reportRequestTrace(T requestTrace) {
		final String requestTraceName = requestTrace.getName();
		if (isReportRequestTraceName(requestTraceName) && !isReportingRateExceeded()) {
			reportingRate.mark();
			final String index = "stagemonitor-requests-" + StringUtils.getLogstashStyleDate();
			final String type = "requests";
			if (!requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()) {
				elasticsearchClient.index(index, type, requestTrace);
			} else {
				requestTraceLogger.info(ElasticsearchClient.getBulkHeader("index", index, type) + JsonUtils.toJson(requestTrace));
			}
		}
	}

	private boolean isReportRequestTraceName(String requestTraceName) {
		final Collection<String> onlyReportRequestsWithName = requestMonitorPlugin.getOnlyReportRequestsWithNameToElasticsearch();
		return onlyReportRequestsWithName.isEmpty() || onlyReportRequestsWithName.contains(requestTraceName);
	}

	private boolean isReportingRateExceeded() {
		final int maxReportingRate = requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch();
		if (Integer.MAX_VALUE == maxReportingRate) {
			return false;
		} else if (maxReportingRate <= 0) {
			return true;
		}
		final double v = 60 * reportingRate.getOneMinuteRate();
		return v > maxReportingRate;
	}

	@Override
	public <T extends RequestTrace> boolean isActive(T requestTrace) {
		return !corePlugin.getElasticsearchUrls().isEmpty();
	}
}
