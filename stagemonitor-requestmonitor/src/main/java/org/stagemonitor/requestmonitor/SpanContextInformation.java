package org.stagemonitor.requestmonitor;

import com.codahale.metrics.Timer;
import com.uber.jaeger.context.TracingUtils;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.sampling.PostExecutionInterceptorContext;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.requestmonitor.tracing.wrapper.StatelessSpanInterceptor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

public class SpanContextInformation {
	private Span span;
	private long overhead1;
	private MonitoredRequest monitoredRequest;
	private SpanContextInformation parent;
	private Future<?> spanReporterFuture;
	private Map<String, Object> requestAttributes = new HashMap<String, Object>();
	private CallStackElement callTree;
	private String operationName;
	private long duration;
	private boolean externalRequest;
	private boolean serverRequest;
	private Timer timerForThisRequest;
	private Map<String, ExternalRequestStats> externalRequestStats = new HashMap<String, ExternalRequestStats>();
	private PostExecutionInterceptorContext postExecutionInterceptorContext;
	private boolean report = true;

	public static SpanContextInformation of(Span span) {
		final SpanContextInformation spanContext = new SpanContextInformation();
		spanContext.setSpan(span);
		return spanContext;
	}

	public static SpanContextInformation of(Span span, String operationName) {
		final SpanContextInformation spanContext = new SpanContextInformation();
		spanContext.setSpan(span);
		spanContext.setOperationName(operationName);
		return spanContext;
	}

	public static SpanContextInformation of(Span span, CallStackElement callTree) {
		return of(span, callTree, Collections.<String, Object>emptyMap());
	}

	public static SpanContextInformation of(Span span, CallStackElement callTree, Map<String, Object> requestAttributes) {
		final SpanContextInformation spanContext = new SpanContextInformation();
		spanContext.setSpan(span);
		spanContext.setCallTree(callTree);
		for (Map.Entry<String, Object> entry : requestAttributes.entrySet()) {
			spanContext.addRequestAttribute(entry.getKey(), entry.getValue());
		}
		return spanContext;
	}

	public String getOperationName() {
		return operationName;
	}

	public Future<?> getSpanReporterFuture() {
		return spanReporterFuture;
	}

	public Span getSpan() {
		return span;
	}

	public void setSpan(Span span) {
		this.span = span;
	}

	/**
	 * Adds an attribute to the request which can later be retrieved by {@link #getRequestAttribute(String)}
	 * <p/>
	 * The attributes won't be reported
	 */
	public void addRequestAttribute(String key, Object value) {
		requestAttributes.put(key, value);
	}

	public Object getRequestAttribute(String key) {
		return requestAttributes.get(key);
	}

	public CallStackElement getCallTree() {
		return callTree;
	}

	private void setOperationName(String operationName) {
		this.operationName = operationName;
	}

	private void setDuration(long duration) {
		this.duration = duration;
	}

	public long getDurationNanos() {
		return duration;
	}

	private void setExternalRequest(boolean externalRequest) {
		this.externalRequest = externalRequest;
	}

	public boolean isExternalRequest() {
		return externalRequest;
	}

	void setServerRequest(boolean serverRequest) {
		this.serverRequest = serverRequest;
	}

	public boolean isServerRequest() {
		return serverRequest;
	}

	public void setCallTree(CallStackElement callTree) {
		this.callTree = callTree;
	}

	public Map<String, Object> getRequestAttributes() {
		return requestAttributes;
	}

	public void setTimerForThisRequest(Timer timerForThisRequest) {
		this.timerForThisRequest = timerForThisRequest;
	}

	public Timer getTimerForThisRequest() {
		return timerForThisRequest;
	}

	public SpanContextInformation getParent() {
		return parent;
	}

	public void setParent(SpanContextInformation parent) {
		this.parent = parent;
	}

	public void addExternalRequest(String requestType, long executionTimeNanos) {
		final ExternalRequestStats stats = this.externalRequestStats.get(requestType);
		if (stats == null) {
			externalRequestStats.put(requestType, new ExternalRequestStats(requestType, executionTimeNanos));
		} else {
			stats.add(executionTimeNanos);
		}
	}

	public Collection<ExternalRequestStats> getExternalRequestStats() {
		return externalRequestStats.values();
	}

	public void setPostExecutionInterceptorContext(PostExecutionInterceptorContext postExecutionInterceptorContext) {
		this.postExecutionInterceptorContext = postExecutionInterceptorContext;
	}

	public PostExecutionInterceptorContext getPostExecutionInterceptorContext() {
		return postExecutionInterceptorContext;
	}

	public boolean isReport() {
		return report;
	}

	public void setReport(boolean report) {
		this.report = report;
	}

	@Deprecated
	public MonitoredRequest getMonitoredRequest() {
		return monitoredRequest;
	}

	@Deprecated
	public void setMonitoredRequest(MonitoredRequest monitoredRequest) {
		this.monitoredRequest = monitoredRequest;
	}

	void setSpanReporterFuture(Future<?> spanReporterFuture) {
		this.spanReporterFuture = spanReporterFuture;
	}

	long getOverhead1() {
		return overhead1;
	}

	void setOverhead1(long overhead1) {
		this.overhead1 = overhead1;
	}

	public static class ExternalRequestStats {

		private final double MS_IN_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

		private final String requestType;
		private int executionCount = 1;
		private long executionTimeNanos;

		ExternalRequestStats(String requestType, long executionTimeNanos) {
			this.requestType = requestType;
			this.executionTimeNanos = executionTimeNanos;
		}

		public double getExecutionTimeMs() {
			return executionTimeNanos / MS_IN_NANOS;
		}

		public long getExecutionTimeNanos() {
			return executionTimeNanos;
		}

		public int getExecutionCount() {
			return executionCount;
		}

		public String getRequestType() {
			return requestType;
		}

		public void add(long executionTimeNanos) {
			executionCount++;
			this.executionTimeNanos += executionTimeNanos;
		}

		public void incrementExecutionTime(long additionalExecutionTime) {
			executionTimeNanos += additionalExecutionTime;
		}
	}

	static class SpanContextSpanInterceptor extends StatelessSpanInterceptor {
		private final RequestMonitor requestMonitor;

		SpanContextSpanInterceptor(RequestMonitor requestMonitor) {
			this.requestMonitor = requestMonitor;
		}

		@Override
		public void onStart(SpanWrapper spanWrapper) {
			TracingUtils.getTraceContext().push(spanWrapper);
			final SpanContextInformation info = requestMonitor.getSpanContext();
			if (info != null) {
				info.setSpan(spanWrapper);
				for (Map.Entry<String, String> entry : Stagemonitor.getMeasurementSession().asMap().entrySet()) {
					spanWrapper.setTag(entry.getKey(), entry.getValue());
				}
			}
		}

		@Override
		public String onSetTag(String key, String value) {
			final SpanContextInformation info = requestMonitor.getSpanContext();
			if (info != null) {
				if (key.equals(Tags.SPAN_KIND.getKey())) {
					info.setExternalRequest(Tags.SPAN_KIND_CLIENT.equals(value));
					info.setServerRequest(Tags.SPAN_KIND_SERVER.equals(value));
				}
			}
			return value;
		}

		@Override
		public Number onSetTag(String key, Number value) {
			final SpanContextInformation info = requestMonitor.getSpanContext();
			if (info != null) {
				if (Tags.SAMPLING_PRIORITY.getKey().equals(key)) {
					info.setReport(value.shortValue() > 0);
				}
			}
			return value;
		}

		@Override
		public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
			TracingUtils.getTraceContext().pop();
			final SpanContextInformation info = requestMonitor.getSpanContext();
			if (info != null) {
				info.setOperationName(operationName);
				info.setDuration(durationNanos);
			}
		}
	}
}
