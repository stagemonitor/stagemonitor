package org.stagemonitor.requestmonitor.reporter;

import java.util.Collection;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

import com.codahale.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
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
	private final Collection<ElasticsearchRequestTraceReporterInterceptor> interceptors =
			new CopyOnWriteArrayList<ElasticsearchRequestTraceReporterInterceptor>();
	private final Configuration configuration;

	public ElasticsearchRequestTraceReporter(Configuration configuration) {
		this(configuration, LoggerFactory.getLogger(ES_REQUEST_TRACE_LOGGER));
	}

	ElasticsearchRequestTraceReporter(Configuration configuration, Logger requestTraceLogger) {
		this.configuration = configuration;
		this.corePlugin = configuration.getConfig(CorePlugin.class);
		this.requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
		this.elasticsearchClient = corePlugin.getElasticsearchClient();
		this.requestTraceLogger = requestTraceLogger;
		this.interceptors.add(new RateLimitingInterceptor());
		this.interceptors.add(new NameFilteringInterceptor());
		this.interceptors.add(new CallTreeExcludingInterceptor());
		for (ElasticsearchRequestTraceReporterInterceptor interceptor : ServiceLoader.load(
				ElasticsearchRequestTraceReporterInterceptor.class,
				ElasticsearchRequestTraceReporter.class.getClassLoader())) {
			interceptors.add(interceptor);
		}

	}

	@Override
	public <T extends RequestTrace> void reportRequestTrace(T requestTrace) {
		InterceptContext context = new InterceptContext(configuration, requestTrace, reportingRate, corePlugin.getMetricRegistry());
		for (ElasticsearchRequestTraceReporterInterceptor interceptor : interceptors) {
			try {
				interceptor.interceptReport(context);
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
		if (context.isReport()) {
			doReport(requestTrace, context);
		}
	}

	private <T extends RequestTrace> void doReport(T requestTrace, InterceptContext context) {
		reportingRate.mark();
		final String index = "stagemonitor-requests-" + StringUtils.getLogstashStyleDate();
		final String type = "requests";
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
	public <T extends RequestTrace> boolean isActive(T requestTrace) {
		return StringUtils.isNotEmpty(corePlugin.getElasticsearchUrl());
	}

	/**
	 * Add an {@link ElasticsearchRequestTraceReporterInterceptor} to the interceptor list
	 *
	 * @param interceptor the interceptor that should be executed before each report
	 */
	public static void registerInterceptor(ElasticsearchRequestTraceReporterInterceptor interceptor) {
		final ElasticsearchRequestTraceReporter thiz = Stagemonitor
				.getConfiguration(RequestMonitorPlugin.class)
				.getRequestMonitor()
				.getReporter(ElasticsearchRequestTraceReporter.class);
		if (thiz != null) {
			thiz.interceptors.add(interceptor);
		}
	}

}
