package org.stagemonitor.requestmonitor.profiler;

import com.uber.jaeger.utils.RateLimiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.tracing.jaeger.RateLimitingPreExecutionInterceptor;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.requestmonitor.tracing.wrapper.StatelessSpanEventListener;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import io.opentracing.Span;

public class CallTreeSpanEventListener extends StatelessSpanEventListener {

	private static final Logger logger = LoggerFactory.getLogger(CallTreeSpanEventListener.class);

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
		final SpanContextInformation contextInfo = requestMonitorPlugin.getRequestMonitor().getSpanContext();
		if (isProfileThisRequest(contextInfo)) {
			contextInfo.setCallTree(Profiler.activateProfiling("total"));
		}
	}

	private boolean isProfileThisRequest(SpanContextInformation spanContext) {
		if (spanContext.isExternalRequest()) {
			return false;
		}
		double callTreeRateLimit = requestMonitorPlugin.getProfilerRateLimitPerMinute();
		if (!requestMonitorPlugin.isProfilerActive()) {
			logger.debug("Not profiling this request because stagemonitor.profiler.active=false");
			return false;
		} else if (callTreeRateLimit <= 0) {
			logger.debug("Not profiling this request because stagemonitor.requestmonitor.onlyReportNRequestsPerMinuteToElasticsearch <= 0");
			return false;
		} else if (rateLimiter != null && rateLimiter.checkCredit(1d)) {
			logger.debug("Not profiling this request because more than {} call trees per minute where created", callTreeRateLimit);
			return false;
		} else if (!spanContext.isReport()) {
			logger.debug("Not profiling this request because this request is not sampled");
			return false;
		}
		return true;
	}

	@Override
	public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
		final SpanContextInformation contextInfo = requestMonitorPlugin.getRequestMonitor().getSpanContext();
		if (contextInfo.getCallTree() != null) {
			try {
				handleCallTree(contextInfo, spanWrapper.getDelegate(), contextInfo.getPostExecutionInterceptorContext().isExcludeCallTree(), operationName);
			} finally {
				Profiler.clearMethodCallParent();
			}
		}
	}

	private void handleCallTree(SpanContextInformation info, Span span, boolean excludeCallTree, String operationName) {
		final CallStackElement callTree = info.getCallTree();
		if (callTree != null) {
			Profiler.stop();
			if (!excludeCallTree && !operationName.isEmpty()) {
				callTree.setSignature(operationName);
				final double minExecutionTimeMultiplier = requestMonitorPlugin.getMinExecutionTimePercent() / 100;
				if (minExecutionTimeMultiplier > 0d) {
					callTree.removeCallsFasterThan((long) (callTree.getExecutionTime() * minExecutionTimeMultiplier));
				}
				span.setTag(SpanUtils.CALL_TREE_JSON, JsonUtils.toJson(callTree));
				span.setTag(SpanUtils.CALL_TREE_ASCII, callTree.toString(true));
			}
		}
	}
}
