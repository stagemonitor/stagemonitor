package org.stagemonitor.requestmonitor.sampling;

import com.codahale.metrics.Meter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;
import org.stagemonitor.requestmonitor.tracing.wrapper.ClientServerAwareSpanInterceptor;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanInterceptor;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import java.util.Collection;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

public class SamplePriorityDeterminingSpanInterceptor extends ClientServerAwareSpanInterceptor implements Callable<SpanInterceptor> {

	private static final Logger logger = LoggerFactory.getLogger(SamplePriorityDeterminingSpanInterceptor.class);
	private final Collection<PreExecutionSpanReporterInterceptor> preInterceptors =
			new CopyOnWriteArrayList<PreExecutionSpanReporterInterceptor>();
	private final Collection<PostExecutionSpanReporterInterceptor> postInterceptors =
			new CopyOnWriteArrayList<PostExecutionSpanReporterInterceptor>();
	private final Configuration configuration;
	private final RequestMonitorPlugin requestMonitorPlugin;
	private final Metric2Registry metricRegistry;
	private final Meter internalRequestReportingRate = new Meter();
	private final Meter externalRequestReportingRate = new Meter();

	public SamplePriorityDeterminingSpanInterceptor(Configuration configuration, Metric2Registry metricRegistry) {
		this.configuration = configuration;
		requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
		this.metricRegistry = metricRegistry;
		registerPreInterceptors();
		registerPostInterceptors();
	}

	private void registerPreInterceptors() {
		this.preInterceptors.add(new RateLimitingPreExecutionInterceptor());

		for (PreExecutionSpanReporterInterceptor interceptor : ServiceLoader.load(
				PreExecutionSpanReporterInterceptor.class,
				SamplePriorityDeterminingSpanInterceptor.class.getClassLoader())) {
			preInterceptors.add(interceptor);
		}
	}

	private void registerPostInterceptors() {
		this.postInterceptors.add(new NameFilteringPostExecutionInterceptor());
		this.postInterceptors.add(new CallTreeExcludingPostExecutionInterceptor());
		this.postInterceptors.add(new FastExternalSpanExcludingPostExecutionInterceptor());

		for (PostExecutionSpanReporterInterceptor interceptor : ServiceLoader.load(
				PostExecutionSpanReporterInterceptor.class,
				SamplePriorityDeterminingSpanInterceptor.class.getClassLoader())) {
			postInterceptors.add(interceptor);
		}
	}

	@Override
	public void onStart(Span span) {
		final RequestMonitor.RequestInformation requestInformation = requestMonitorPlugin.getRequestMonitor().getRequestInformation();
		PreExecutionInterceptorContext context = new PreExecutionInterceptorContext(configuration,
				requestInformation, internalRequestReportingRate,
				externalRequestReportingRate, metricRegistry);
		for (PreExecutionSpanReporterInterceptor interceptor : preInterceptors) {
			try {
				interceptor.interceptReport(context);
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
		requestInformation.setPreExecutionInterceptorContext(context);

		if (!context.isReport()) {
			Tags.SAMPLING_PRIORITY.set(span, (short) 0);
		}
	}

	@Override
	public void onFinish(Span span, String operationName, long durationNanos) {
		final RequestMonitor.RequestInformation info = requestMonitorPlugin.getRequestMonitor().getRequestInformation();
		if (!info.getPreExecutionInterceptorContext().isReport()) {
			return;
		}
		PostExecutionInterceptorContext context = new PostExecutionInterceptorContext(configuration, info,
				internalRequestReportingRate, externalRequestReportingRate, metricRegistry);
		for (PostExecutionSpanReporterInterceptor interceptor : postInterceptors) {
			try {
				interceptor.interceptReport(context);
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
		info.setPostExecutionInterceptorContext(context);
		handleCallTree(info, span, context.isExcludeCallTree(), operationName);
		if (context.isReport()) {
			if (isClient) {
				externalRequestReportingRate.mark();
			} else {
				internalRequestReportingRate.mark();
			}
		} else {
			Tags.SAMPLING_PRIORITY.set(span, (short) 0);
		}
	}

	private void handleCallTree(RequestMonitor.RequestInformation info, Span span, boolean excludeCallTree, String operationName) {
		final CallStackElement callTree = info.getCallTree();
		if (callTree != null) {
			Profiler.stop();
			if (!excludeCallTree) {
				callTree.setSignature(operationName);
				final double minExecutionTimeMultiplier = requestMonitorPlugin.getMinExecutionTimePercent() / 100;
				if (minExecutionTimeMultiplier > 0d) {
					callTree.removeCallsFasterThan((long) (callTree.getExecutionTime() * minExecutionTimeMultiplier));
				}
				SpanUtils.setCallTree(span, callTree);
			}
		}
	}

	@Override
	public SpanInterceptor call() throws Exception {
		return this;
	}

	public void addPreInterceptor(PreExecutionSpanReporterInterceptor interceptor) {
		preInterceptors.add(interceptor);
	}

	public void addPostInterceptor(PostExecutionSpanReporterInterceptor interceptor) {
		postInterceptors.add(interceptor);
	}
}
