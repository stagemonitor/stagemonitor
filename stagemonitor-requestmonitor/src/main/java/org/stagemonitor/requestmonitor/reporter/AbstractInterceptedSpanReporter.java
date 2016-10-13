package org.stagemonitor.requestmonitor.reporter;

import com.codahale.metrics.Meter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
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
	private Meter reportingRate = new Meter();
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
	public void report(ReportArguments reportArguments) {
		PostExecutionInterceptorContext context = new PostExecutionInterceptorContext(configuration, reportArguments.getRequestTrace(),
				reportArguments.getSpan(), reportingRate, corePlugin.getMetricRegistry());
		for (PostExecutionRequestTraceReporterInterceptor interceptor : postInterceptors) {
			try {
				interceptor.interceptReport(context);
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
		if (context.isReport()) {
			reportingRate.mark();
			doReport(reportArguments, context);
		}
	}

	protected abstract void doReport(ReportArguments reportArguments, PostExecutionInterceptorContext context);

	@Override
	public boolean isActive(IsActiveArguments isActiveArguments) {
		PreExecutionInterceptorContext context = new PreExecutionInterceptorContext(configuration, isActiveArguments.getRequestTrace(),
				isActiveArguments.getSpan(), reportingRate, corePlugin.getMetricRegistry());
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
