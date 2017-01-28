package org.stagemonitor.requestmonitor.reporter;

import com.codahale.metrics.Meter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

import java.util.Collection;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractInterceptedSpanReporter extends SpanReporter {
	protected CorePlugin corePlugin;
	protected RequestMonitorPlugin requestMonitorPlugin;
	protected ElasticsearchClient elasticsearchClient;
	protected Collection<PreExecutionRequestTraceReporterInterceptor> preInterceptors =
			new CopyOnWriteArrayList<PreExecutionRequestTraceReporterInterceptor>();
	protected Collection<PostExecutionRequestTraceReporterInterceptor> postInterceptors =
			new CopyOnWriteArrayList<PostExecutionRequestTraceReporterInterceptor>();
	protected Configuration configuration;
	private Meter internalRequestReportingRate = new Meter();
	private Meter externalRequestReportingRate = new Meter();
	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void init(InitArguments initArguments) {
		this.configuration = initArguments.getConfiguration();
		this.corePlugin = configuration.getConfig(CorePlugin.class);
		this.elasticsearchClient = corePlugin.getElasticsearchClient();
		this.requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
		registerPreInterceptors();
		registerPostInterceptors();
	}

	private void registerPreInterceptors() {
		this.preInterceptors.add(new RateLimitingPreExecutionInterceptor());

		for (PreExecutionRequestTraceReporterInterceptor interceptor : ServiceLoader.load(
				PreExecutionRequestTraceReporterInterceptor.class,
				AbstractInterceptedSpanReporter.class.getClassLoader())) {
			preInterceptors.add(interceptor);
		}
	}

	private void registerPostInterceptors() {
		this.postInterceptors.add(new NameFilteringPostExecutionInterceptor());
		this.postInterceptors.add(new CallTreeExcludingPostExecutionInterceptor());
		this.postInterceptors.add(new FastExternalSpanExcludingPostExecutionInterceptor());

		for (PostExecutionRequestTraceReporterInterceptor interceptor : ServiceLoader.load(
				PostExecutionRequestTraceReporterInterceptor.class,
				AbstractInterceptedSpanReporter.class.getClassLoader())) {
			postInterceptors.add(interceptor);
		}
	}

	@Override
	public void report(RequestMonitor.RequestInformation requestInformation) {
		PostExecutionInterceptorContext context = new PostExecutionInterceptorContext(configuration,
				requestInformation, internalRequestReportingRate, externalRequestReportingRate, corePlugin.getMetricRegistry());
		for (PostExecutionRequestTraceReporterInterceptor interceptor : postInterceptors) {
			try {
				interceptor.interceptReport(context);
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
		if (context.isReport()) {
			if (requestInformation.isExternalRequest()) {
				externalRequestReportingRate.mark();
			} else {
				internalRequestReportingRate.mark();
			}
			doReport(requestInformation, context);
		}
	}

	protected abstract void doReport(RequestMonitor.RequestInformation requestInformation, PostExecutionInterceptorContext context);

	@Override
	public boolean isActive(RequestMonitor.RequestInformation requestInformation) {
		PreExecutionInterceptorContext context = new PreExecutionInterceptorContext(configuration,
				requestInformation, internalRequestReportingRate, externalRequestReportingRate, corePlugin.getMetricRegistry());
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
