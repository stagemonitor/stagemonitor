package org.stagemonitor.tracing.profiler;

import com.codahale.metrics.Timer;

import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.core.metrics.MetricUtils;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.metrics.MetricsSpanEventListener;
import org.stagemonitor.tracing.sampling.PreExecutionInterceptorContext;
import org.stagemonitor.tracing.sampling.RateLimitingPreExecutionInterceptor;
import org.stagemonitor.tracing.utils.RateLimiter;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.tracing.wrapper.StatelessSpanEventListener;
import org.stagemonitor.util.StringUtils;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

public class CallTreeSpanEventListener extends StatelessSpanEventListener {

	private final TracingPlugin tracingPlugin;
	private RateLimiter rateLimiter;
	private Metric2Registry metricRegistry;

	public CallTreeSpanEventListener(Metric2Registry metricRegistry, TracingPlugin tracingPlugin) {
		this.tracingPlugin = tracingPlugin;
		this.metricRegistry = metricRegistry;
		rateLimiter = RateLimitingPreExecutionInterceptor.getRateLimiter(tracingPlugin.getProfilerRateLimitPerMinute());
		tracingPlugin.getProfilerRateLimitPerMinuteOption().addChangeListener(new ConfigurationOption.ChangeListener<Double>() {
			@Override
			public void onChange(ConfigurationOption<?> configurationOption, Double oldValue, Double newValue) {
				rateLimiter = RateLimitingPreExecutionInterceptor.getRateLimiter(newValue);
			}
		});
	}

	@Override
	public void onStart(SpanWrapper spanWrapper) {
		final SpanContextInformation contextInfo = SpanContextInformation.forSpan(spanWrapper);
		if (tracingPlugin.isSampled(spanWrapper) && contextInfo.getPreExecutionInterceptorContext() != null) {
			determineIfEnableProfiler(spanWrapper, contextInfo);
			if (!Profiler.isProfilingActive() && contextInfo.getPreExecutionInterceptorContext().isCollectCallTree()) {
				contextInfo.setCallTree(Profiler.activateProfiling("total"));
			}
		}
	}

	private void determineIfEnableProfiler(SpanWrapper spanWrapper, SpanContextInformation spanContext) {
		final PreExecutionInterceptorContext interceptorContext = spanContext.getPreExecutionInterceptorContext();
		if (Tags.SPAN_KIND_CLIENT.equals(spanWrapper.getStringTag(Tags.SPAN_KIND.getKey()))) {
			interceptorContext.shouldNotCollectCallTree("this is a external request (span.kind=client)");
			return;
		}
		double callTreeRateLimit = tracingPlugin.getProfilerRateLimitPerMinute();
		if (!tracingPlugin.isProfilerActive()) {
			interceptorContext.shouldNotCollectCallTree("stagemonitor.profiler.active=false");
		} else if (callTreeRateLimit <= 0) {
			interceptorContext.shouldNotCollectCallTree("stagemonitor.profiler.sampling.rateLimitPerMinute <= 0");
		} else if (RateLimitingPreExecutionInterceptor.isRateExceeded(rateLimiter)) {
			interceptorContext.shouldNotCollectCallTree("rate limit is reached");
		}
	}

	@Override
	public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
		final SpanContextInformation contextInfo = SpanContextInformation.forSpan(spanWrapper);
		if (contextInfo.getCallTree() != null) {
			try {
				Profiler.stop();
				if (tracingPlugin.isSampled(spanWrapper)) {
					determineIfExcludeCallTree(contextInfo);
					if (isAddCallTreeToSpan(contextInfo, operationName)) {
						addCallTreeToSpan(contextInfo, spanWrapper, operationName);
					}
				}
			} finally {
				Profiler.clearMethodCallParent();
			}
		}
	}

	private void determineIfExcludeCallTree(SpanContextInformation contextInfo) {
		final double percentileLimit = tracingPlugin.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests();
		final Timer timer = MetricsSpanEventListener.getTimer(metricRegistry, contextInfo);
		if (timer != null && !MetricUtils.isFasterThanXPercentOfAllRequests(contextInfo.getDurationNanos(), percentileLimit, timer)) {
			contextInfo.getPostExecutionInterceptorContext().excludeCallTree("the duration of this request is faster than the percentile limit");
		}
	}

	private boolean isAddCallTreeToSpan(SpanContextInformation info, String operationName) {
		return info.getCallTree() != null
				&& info.getPostExecutionInterceptorContext() != null
				&& !info.getPostExecutionInterceptorContext().isExcludeCallTree()
				&& StringUtils.isNotEmpty(operationName);
	}

	private void addCallTreeToSpan(SpanContextInformation info, Span span, String operationName) {
		final CallStackElement callTree = info.getCallTree();
		callTree.setSignature(operationName);
		final double minExecutionTimeMultiplier = tracingPlugin.getMinExecutionTimePercent() / 100;
		if (minExecutionTimeMultiplier > 0d) {
			callTree.removeCallsFasterThan((long) (callTree.getExecutionTime() * minExecutionTimeMultiplier));
		}
		if (!tracingPlugin.getExcludedTags().contains(SpanUtils.CALL_TREE_JSON)) {
			span.setTag(SpanUtils.CALL_TREE_JSON, JsonUtils.toJson(callTree));
		}
		if (!tracingPlugin.getExcludedTags().contains(SpanUtils.CALL_TREE_ASCII)) {
			span.setTag(SpanUtils.CALL_TREE_ASCII, callTree.toString(true, tracingPlugin.getCallTreeAsciiFormatter()));
		}
	}
}
