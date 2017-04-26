package org.stagemonitor.tracing;

import com.codahale.metrics.Timer;
import com.uber.jaeger.context.TraceContext;
import com.uber.jaeger.context.TracingUtils;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.WeakConcurrentMap;
import org.stagemonitor.tracing.profiler.CallStackElement;
import org.stagemonitor.tracing.reporter.ReadbackSpan;
import org.stagemonitor.tracing.sampling.PostExecutionInterceptorContext;
import org.stagemonitor.tracing.sampling.PreExecutionInterceptorContext;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.AbstractSpanEventListener;
import org.stagemonitor.tracing.wrapper.SpanEventListener;
import org.stagemonitor.tracing.wrapper.SpanEventListenerFactory;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

public class SpanContextInformation {

	private static final WeakConcurrentMap<Span, SpanContextInformation> spanContextMap = new WeakConcurrentMap
			.WithInlinedExpunction<Span, SpanContextInformation>();

	private Span span;
	private long overhead1;
	private SpanContextInformation parent;
	private Map<String, Object> requestAttributes = new HashMap<String, Object>();
	private CallStackElement callTree;
	private String operationName;
	private long duration;
	private boolean externalRequest;
	private boolean serverRequest;
	private Timer timerForThisRequest;
	private Map<String, ExternalRequestStats> externalRequestStats = new HashMap<String, ExternalRequestStats>();
	private PostExecutionInterceptorContext postExecutionInterceptorContext;
	private boolean sampled = true;
	private PreExecutionInterceptorContext preExecutionInterceptorContext;
	private String operationType;
	private ReadbackSpan readbackSpan;

	public static SpanContextInformation getCurrent() {
		final TraceContext traceContext = TracingUtils.getTraceContext();
		if (traceContext.isEmpty()) {
			return null;
		}
		return forSpan(traceContext.getCurrentSpan());
	}

	/**
	 * Gets or creates the {@link SpanContextInformation} for the provided span.
	 */
	public static SpanContextInformation forSpan(Span span) {
		if (span != null) {
			if (!spanContextMap.containsKey(span)) {
				spanContextMap.putIfAbsent(span, SpanContextInformation.forUnitTest(span));
			}
			return spanContextMap.get(span);
		}
		return null;
	}

	/**
	 * Internal method, should only be called by stagemonitor tests
	 */
	public static SpanContextInformation forUnitTest(Span span) {
		final SpanContextInformation spanContext = new SpanContextInformation();
		spanContext.setSpan(span);
		return spanContext;
	}

	/**
	 * Internal method, should only be called by stagemonitor tests
	 */
	public static SpanContextInformation forUnitTest(ReadbackSpan span) {
		final SpanContextInformation spanContext = new SpanContextInformation();
		spanContext.setReadbackSpan(span);
		return spanContext;
	}

	/**
	 * Internal method, should only be called by stagemonitor tests
	 */
	public static SpanContextInformation forUnitTest(Span span, String operationName) {
		final SpanContextInformation spanContext = new SpanContextInformation();
		spanContext.setSpan(span);
		spanContext.setOperationName(operationName);
		return spanContext;
	}

	/**
	 * Internal method, should only be called by stagemonitor tests
	 */
	public static SpanContextInformation forUnitTest(Span span, Map<String, Object> requestAttributes) {
		final SpanContextInformation spanContext = new SpanContextInformation();
		spanContext.setSpan(span);
		for (Map.Entry<String, Object> entry : requestAttributes.entrySet()) {
			spanContext.addRequestAttribute(entry.getKey(), entry.getValue());
		}
		return spanContext;
	}

	public String getOperationName() {
		return operationName;
	}

	public Span getSpan() {
		return span;
	}

	/**
	 * Internal method, should only be called by stagemonitor itself
	 */
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

	/**
	 * Internal method, should only be called by stagemonitor itself
	 */
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

	/**
	 * Internal method, should only be called by stagemonitor itself
	 */
	public void setPostExecutionInterceptorContext(PostExecutionInterceptorContext postExecutionInterceptorContext) {
		this.postExecutionInterceptorContext = postExecutionInterceptorContext;
	}

	public PostExecutionInterceptorContext getPostExecutionInterceptorContext() {
		return postExecutionInterceptorContext;
	}

	public boolean isSampled() {
		return sampled;
	}

	/**
	 * Internal method, should only be called by stagemonitor itself
	 */
	public void setSampled(boolean sampled) {
		this.sampled = sampled;
	}

	long getOverhead1() {
		return overhead1;
	}

	void setOverhead1(long overhead1) {
		this.overhead1 = overhead1;
	}

	@Override
	public void finalize() throws Throwable {
		super.finalize();
		if (callTree != null) {
			callTree.recycle();
		}
	}

	/**
	 * Internal method, should only be called by stagemonitor itself
	 */
	public void setPreExecutionInterceptorContext(PreExecutionInterceptorContext preExecutionInterceptorContext) {
		this.preExecutionInterceptorContext = preExecutionInterceptorContext;
	}

	public PreExecutionInterceptorContext getPreExecutionInterceptorContext() {
		return preExecutionInterceptorContext;
	}

	public String getOperationType() {
		return operationType;
	}

	public void setReadbackSpan(ReadbackSpan readbackSpan) {
		this.readbackSpan = readbackSpan;
	}

	public ReadbackSpan getReadbackSpan() {
		return readbackSpan;
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
	}

	static class SpanContextSpanEventListener extends AbstractSpanEventListener implements SpanEventListenerFactory {

		private SpanContextInformation info;
		private String spanKind;
		private Number samplingPriority;
		private String operationType;

		@Override
		public void onStart(SpanWrapper spanWrapper) {
			info = SpanContextInformation.forSpan(spanWrapper);
			info.setParent(SpanContextInformation.getCurrent());

			TracingUtils.getTraceContext().push(spanWrapper);
			spanContextMap.put(spanWrapper, info);
			info.setSpan(spanWrapper);
			for (Map.Entry<String, String> entry : Stagemonitor.getMeasurementSession().asMap().entrySet()) {
				spanWrapper.setTag(entry.getKey(), entry.getValue());
			}

			handleTagsSetBeforeSpanStarted();
		}

		private void handleTagsSetBeforeSpanStarted() {
			if (spanKind != null) {
				onSetTag(Tags.SPAN_KIND.getKey(), spanKind);
			}
			if (samplingPriority != null) {
				onSetTag(Tags.SAMPLING_PRIORITY.getKey(), samplingPriority);
			}
			if (operationType != null) {
				onSetTag(SpanUtils.OPERATION_TYPE, operationType);
			}
		}

		@Override
		public String onSetTag(String key, String value) {
			if (key.equals(Tags.SPAN_KIND.getKey())) {
				if (info != null) {
					info.setExternalRequest(Tags.SPAN_KIND_CLIENT.equals(value));
					info.setServerRequest(Tags.SPAN_KIND_SERVER.equals(value));
				} else {
					// span.kind was set before the span has been started
					// store in instance variable and set again if a SpanContextInformation is available
					spanKind = value;
				}
			} else if (SpanUtils.OPERATION_TYPE.equals(key)) {
				if (info != null) {
					info.operationType = value;
				} else {
					operationType = value;
				}
			}
			return value;
		}

		@Override
		public Number onSetTag(String key, Number value) {
			if (Tags.SAMPLING_PRIORITY.getKey().equals(key)) {
				if (info != null) {
					info.setSampled(value.shortValue() > 0);
				} else {
					// sampling.priority was set before the span has been started
					// store in instance variable and set again if a SpanContextInformation is available
					samplingPriority = value;
				}
			}
			return value;
		}

		@Override
		public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
			TracingUtils.getTraceContext().pop();
			final SpanContextInformation info = SpanContextInformation.forSpan(spanWrapper);
			info.setOperationName(operationName);
			info.setDuration(durationNanos);
		}

		@Override
		public SpanEventListener create() {
			return new SpanContextSpanEventListener();
		}
	}
}
