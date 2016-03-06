package org.stagemonitor.requestmonitor.reporter;

import java.util.Collection;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

import com.codahale.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;

public abstract class AbstractInterceptedRequestTraceReporter implements RequestTraceReporter {
	protected final CorePlugin corePlugin;
	protected final RequestMonitorPlugin requestMonitorPlugin;
	protected final ElasticsearchClient elasticsearchClient;
	protected final Collection<PreExecutionRequestTraceReporterInterceptor> preInterceptors =
			new CopyOnWriteArrayList<PreExecutionRequestTraceReporterInterceptor>();
	protected final Collection<PostExecutionRequestTraceReporterInterceptor> postInterceptors =
			new CopyOnWriteArrayList<PostExecutionRequestTraceReporterInterceptor>();
	protected final Configuration configuration;
	private final Meter reportingRate = new Meter();
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public AbstractInterceptedRequestTraceReporter(Configuration configuration) {
		this.corePlugin = configuration.getConfig(CorePlugin.class);
		this.elasticsearchClient = corePlugin.getElasticsearchClient();
		this.requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
		this.configuration = configuration;
		registerPreInterceptors();
		registerPostInterceptors();
	}

	private void registerPreInterceptors() {
		this.preInterceptors.add(new RateLimitingPreExecutionInterceptor());

		for (PostExecutionRequestTraceReporterInterceptor interceptor : ServiceLoader.load(
				PostExecutionRequestTraceReporterInterceptor.class,
				ElasticsearchRequestTraceReporter.class.getClassLoader())) {
			postInterceptors.add(interceptor);
		}
	}

	private void registerPostInterceptors() {
		this.postInterceptors.add(new NameFilteringPostExecutionInterceptor());
		this.postInterceptors.add(new CallTreeExcludingPostExecutionInterceptor());

		for (PreExecutionRequestTraceReporterInterceptor interceptor : ServiceLoader.load(
				PreExecutionRequestTraceReporterInterceptor.class,
				ElasticsearchRequestTraceReporter.class.getClassLoader())) {
			preInterceptors.add(interceptor);
		}
	}

	@Override
	public <T extends RequestTrace> void reportRequestTrace(T requestTrace) {
		PostExecutionInterceptorContext context = new PostExecutionInterceptorContext(configuration, requestTrace,
				reportingRate, corePlugin.getMetricRegistry());
		for (PostExecutionRequestTraceReporterInterceptor interceptor : postInterceptors) {
			try {
				interceptor.interceptReport(context);
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
		if (context.isReport()) {
			reportingRate.mark();
			doReport(requestTrace, context);
		}
	}

	protected abstract <T extends RequestTrace> void doReport(T requestTrace, PostExecutionInterceptorContext context);

	@Override
	public <T extends RequestTrace> boolean isActive(T requestTrace) {
		PreExecutionInterceptorContext context = new PreExecutionInterceptorContext(configuration, requestTrace,
				reportingRate, corePlugin.getMetricRegistry());
		for (PreExecutionRequestTraceReporterInterceptor interceptor : preInterceptors) {
			try {
				interceptor.interceptReport(context);
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
		return context.isReport();
	}
}
