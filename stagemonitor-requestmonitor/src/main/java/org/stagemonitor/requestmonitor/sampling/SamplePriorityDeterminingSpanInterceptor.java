package org.stagemonitor.requestmonitor.sampling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.requestmonitor.tracing.wrapper.StatelessSpanInterceptor;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import java.util.Collection;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

public class SamplePriorityDeterminingSpanInterceptor extends StatelessSpanInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(SamplePriorityDeterminingSpanInterceptor.class);
	private final Collection<PreExecutionSpanInterceptor> preInterceptors =
			new CopyOnWriteArrayList<PreExecutionSpanInterceptor>();
	private final Collection<PostExecutionSpanInterceptor> postInterceptors =
			new CopyOnWriteArrayList<PostExecutionSpanInterceptor>();
	private final Configuration configuration;
	private final RequestMonitorPlugin requestMonitorPlugin;
	private final Metric2Registry metricRegistry;

	public SamplePriorityDeterminingSpanInterceptor(Configuration configuration, Metric2Registry metricRegistry) {
		this.configuration = configuration;
		requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
		this.metricRegistry = metricRegistry;
		registerPreInterceptors();
		registerPostInterceptors();
	}

	private void registerPreInterceptors() {
		for (PreExecutionSpanInterceptor interceptor : ServiceLoader.load(
				PreExecutionSpanInterceptor.class,
				SamplePriorityDeterminingSpanInterceptor.class.getClassLoader())) {
			addPreInterceptor(interceptor);
		}
	}

	private void registerPostInterceptors() {
		addPostInterceptor(new NameFilteringPostExecutionInterceptor());
		addPostInterceptor(new CallTreeExcludingPostExecutionInterceptor());
		addPostInterceptor(new FastExternalSpanExcludingPostExecutionInterceptor());

		for (PostExecutionSpanInterceptor interceptor : ServiceLoader.load(
				PostExecutionSpanInterceptor.class,
				SamplePriorityDeterminingSpanInterceptor.class.getClassLoader())) {
			addPostInterceptor(interceptor);
		}
	}

	@Override
	public void onStart(SpanWrapper spanWrapper) {
		final SpanContextInformation spanContext = requestMonitorPlugin.getRequestMonitor().getSpanContext();
		if (!spanContext.isReport()) {
			return;
		}

		PreExecutionInterceptorContext context = new PreExecutionInterceptorContext(configuration,
				spanContext, metricRegistry);
		for (PreExecutionSpanInterceptor interceptor : preInterceptors) {
			try {
				interceptor.interceptReport(context);
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}

		if (!context.isReport()) {
			spanContext.setReport(false);
			Tags.SAMPLING_PRIORITY.set(spanWrapper, (short) 0);
		}
	}

	@Override
	public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
		final SpanContextInformation info = requestMonitorPlugin.getRequestMonitor().getSpanContext();
		if (!info.isReport()) {
			return;
		}
		PostExecutionInterceptorContext context = new PostExecutionInterceptorContext(configuration, info, metricRegistry);
		for (PostExecutionSpanInterceptor interceptor : postInterceptors) {
			try {
				interceptor.interceptReport(context);
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
		info.setPostExecutionInterceptorContext(context);
		handleCallTree(info, spanWrapper.getDelegate(), context.isExcludeCallTree(), operationName);
		if (!context.isReport()) {
			info.setReport(false);
			Tags.SAMPLING_PRIORITY.set(spanWrapper.getDelegate(), (short) 0);
		}
	}

	private void handleCallTree(SpanContextInformation info, Span span, boolean excludeCallTree, String operationName) {
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

	public void addPreInterceptor(PreExecutionSpanInterceptor interceptor) {
		preInterceptors.add(interceptor);
	}

	public void addPostInterceptor(PostExecutionSpanInterceptor interceptor) {
		postInterceptors.add(interceptor);
	}
}
