package org.stagemonitor.requestmonitor.reporter;

import java.util.LinkedList;
import java.util.ServiceLoader;

import com.codahale.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;

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
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Iterable<ElasticsearchRequestTraceReporterInterceptor> interceptors;

	public ElasticsearchRequestTraceReporter(Configuration configuration) {
		this(configuration, LoggerFactory.getLogger(ES_REQUEST_TRACE_LOGGER));
	}

	ElasticsearchRequestTraceReporter(Configuration configuration, Logger requestTraceLogger) {
		this.corePlugin = configuration.getConfig(CorePlugin.class);
		this.requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
		this.elasticsearchClient = corePlugin.getElasticsearchClient();
		this.requestTraceLogger = requestTraceLogger;
		this.interceptors = ServiceLoader.load(ElasticsearchRequestTraceReporterInterceptor.class,
				ElasticsearchRequestTraceReporter.class.getClassLoader());
		for (ElasticsearchRequestTraceReporterInterceptor interceptor : interceptors) {
			interceptor.init(configuration);
		}
	}

	@Override
	public <T extends RequestTrace> void reportRequestTrace(T requestTrace) {
		final LinkedList<String> excludedProperties = new LinkedList<String>();
		for (ElasticsearchRequestTraceReporterInterceptor interceptor : interceptors) {
			if (!interceptor.interceptReport(requestTrace, reportingRate, excludedProperties)) {
				logger.debug("{} aborted reporting a request trace to Elasticsearch", interceptor);
				return;
			}
		}
		doReport(requestTrace, excludedProperties);
	}

	private <T extends RequestTrace> void doReport(T requestTrace, LinkedList<String> excludedProperties) {
		reportingRate.mark();
		final String index = "stagemonitor-requests-" + StringUtils.getLogstashStyleDate();
		final String type = "requests";
		if (!requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()) {
			if (excludedProperties.isEmpty()) {
				elasticsearchClient.index(index, type, requestTrace);
			} else {
				elasticsearchClient.index(index, type, JsonUtils.toObjectNode(requestTrace).remove(excludedProperties));
			}
		} else {
			requestTraceLogger.info(ElasticsearchClient.getBulkHeader("index", index, type) + JsonUtils.toJson(requestTrace));
		}
	}

	@Override
	public <T extends RequestTrace> boolean isActive(T requestTrace) {
		return StringUtils.isNotEmpty(corePlugin.getElasticsearchUrl());
	}

}
