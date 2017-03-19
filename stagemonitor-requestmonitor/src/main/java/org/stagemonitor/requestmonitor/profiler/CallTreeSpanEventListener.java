package org.stagemonitor.requestmonitor.profiler;

import com.uber.jaeger.utils.RateLimiter;

import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.metrics.MetricUtils;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.sampling.PreExecutionInterceptorContext;
import org.stagemonitor.requestmonitor.tracing.jaeger.RateLimitingPreExecutionInterceptor;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.requestmonitor.tracing.wrapper.StatelessSpanEventListener;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import io.opentracing.Span;

public class CallTreeSpanEventListener extends StatelessSpanEventListener {

	private final RequestMonitorPlugin requestMonitorPlugin;
	private RateLimiter rateLimiter;

	public CallTreeSpanEventListener(RequestMonitorPlugin requestMonitorPlugin) {
		this.requestMonitorPlugin = requestMonitorPlugin;
		rateLimiter = RateLimitingPreExecutionInterceptor.getRateLimiter(requestMonitorPlugin.getProfilerRateLimitPerMinute());
		requestMonitorPlugin.getProfilerRateLimitPerMinuteOption().addChangeListener(new ConfigurationOption.ChangeListener<Double>() {
			@Override
			public void onChange(ConfigurationOption<?> configurationOption, Double oldValue, Double newValue) {
				rateLimiter = RateLimitingPreExecutionInterceptor.getRateLimiter(newValue);
			}
		});
	}

	@Override
	public void onStart(SpanWrapper spanWrapper) {
		final SpanContextInformation contextInfo = SpanContextInformation.forSpan(spanWrapper);
		if (contextInfo.isSampled() && contextInfo.getPreExecutionInterceptorContext() != null) {
			determineIfEnableProfiler(contextInfo);
			if (contextInfo.getPreExecutionInterceptorContext().isCollectCallTree()) {
				contextInfo.setCallTree(Profiler.activateProfiling("total"));
			}
		}
	}

	private void determineIfEnableProfiler(SpanContextInformation spanContext) {
		final PreExecutionInterceptorContext interceptorContext = spanContext.getPreExecutionInterceptorContext();
		if (spanContext.isExternalRequest()) {
			interceptorContext.shouldNotCollectCallTree("this is a external request (span.kind=client)");
			return;
		}
		double callTreeRateLimit = requestMonitorPlugin.getProfilerRateLimitPerMinute();
		if (!requestMonitorPlugin.isProfilerActive()) {
			interceptorContext.shouldNotCollectCallTree("stagemonitor.profiler.active=false");
		} else if (callTreeRateLimit <= 0) {
			interceptorContext.shouldNotCollectCallTree("stagemonitor.profiler.sampling.rateLimitPerMinute <= 0");
		} else if (rateLimiter != null && rateLimiter.checkCredit(1d)) {
			interceptorContext.shouldNotCollectCallTree("rate limit is reached");
		}
	}

	@Override
	public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
		final SpanContextInformation contextInfo = SpanContextInformation.forSpan(spanWrapper);
		if (contextInfo.getCallTree() != null) {
			try {
				Profiler.stop();
				if (contextInfo.isSampled()) {
					determineIfExcludeCallTree(contextInfo);
					if (isAddCallTreeToSpan(contextInfo, operationName)) {
						addCallTreeToSpan(contextInfo, spanWrapper.getDelegate(), operationName);
					}
				}
			} finally {
				Profiler.clearMethodCallParent();
			}
		}
	}

	private void determineIfExcludeCallTree(SpanContextInformation contextInfo) {
		final double percentileLimit = requestMonitorPlugin.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests();
		if (!MetricUtils.isFasterThanXPercentOfAllRequests(contextInfo.getDurationNanos(), percentileLimit, contextInfo.getTimerForThisRequest())) {
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
		final double minExecutionTimeMultiplier = requestMonitorPlugin.getMinExecutionTimePercent() / 100;
		if (minExecutionTimeMultiplier > 0d) {
			callTree.removeCallsFasterThan((long) (callTree.getExecutionTime() * minExecutionTimeMultiplier));
		}
		span.setTag(SpanUtils.CALL_TREE_JSON, JsonUtils.toJson(callTree));
		span.setTag(SpanUtils.CALL_TREE_ASCII, callTree.toString(true));
	}
}
