package org.stagemonitor.tracing;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.tracing.profiler.CallStackElement;
import org.stagemonitor.tracing.sampling.PostExecutionInterceptorContext;
import org.stagemonitor.tracing.sampling.PreExecutionInterceptorContext;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.AbstractSpanEventListener;
import org.stagemonitor.tracing.wrapper.SpanEventListener;
import org.stagemonitor.tracing.wrapper.SpanEventListenerFactory;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.tracing.wrapper.StatelessSpanEventListener;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;

public class SpanContextInformation {

	private static final ConcurrentHashMap<Span, SpanContextInformation> spanContextMap =
			new ConcurrentHashMap<Span, SpanContextInformation>();

	private long overhead1;
	private SpanContextInformation parent;
	private Map<String, Object> requestAttributes = new HashMap<String, Object>();
	private CallStackElement callTree;
	private Map<String, ExternalRequestStats> externalRequestStats = new HashMap<String, ExternalRequestStats>();
	private PostExecutionInterceptorContext postExecutionInterceptorContext;
	private PreExecutionInterceptorContext preExecutionInterceptorContext;
	private SpanWrapper spanWrapper;

	public static SpanContextInformation getCurrent() {
		final Scope activeScope = GlobalTracer.get().scopeManager().active();
		if (activeScope == null) {
			return null;
		}
		return forSpan(activeScope.span());
	}

	/**
	 * Gets or creates the {@link SpanContextInformation} for the provided span.
	 */
	public static SpanContextInformation forSpan(Span span) {
		if (span != null) {
			if (!spanContextMap.containsKey(span)) {
				spanContextMap.putIfAbsent(span, new SpanContextInformation());
			}
			return spanContextMap.get(span);
		}
		return null;
	}

	public static SpanContextInformation get(Span span) {
		return spanContextMap.get(span);
	}

	public String getOperationName() {
		return getSpanWrapper().getOperationName();
	}

	/**
	 * Adds an attribute to the request which can later be retrieved by {@link #getRequestAttribute(String)}
	 * <p>
	 * The attributes won't be reported
	 */
	public void addRequestAttribute(String key, Object value) {
		requestAttributes.put(key, value);
	}

	public <T> T getRequestAttribute(String key) {
		return (T) requestAttributes.get(key);
	}

	public CallStackElement getCallTree() {
		return callTree;
	}

	public long getDurationNanos() {
		return getSpanWrapper().getDurationNanos();
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
		return getSpanWrapper().getStringTag(SpanUtils.OPERATION_TYPE);
	}

	public SpanWrapper getSpanWrapper() {
		return spanWrapper;
	}

	public static class ExternalRequestStats {

		private final String requestType;
		private int executionCount = 1;
		private long executionTimeNanos;

		ExternalRequestStats(String requestType, long executionTimeNanos) {
			this.requestType = requestType;
			this.executionTimeNanos = executionTimeNanos;
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

	public static class SpanContextSpanEventListener extends AbstractSpanEventListener implements SpanEventListenerFactory {

		private SpanContextInformation info;

		@Override
		public void onStart(SpanWrapper spanWrapper) {
			info = SpanContextInformation.forSpan(spanWrapper);
			info.spanWrapper = spanWrapper;
			info.setParent(SpanContextInformation.getCurrent());

			spanContextMap.put(spanWrapper, info);
			for (Map.Entry<String, String> entry : Stagemonitor.getMeasurementSession().asMap().entrySet()) {
				spanWrapper.setTag(entry.getKey(), entry.getValue());
			}
		}

		@Override
		public SpanEventListener create() {
			return new SpanContextSpanEventListener();
		}
	}

	public static class SpanFinalizer extends StatelessSpanEventListener {
		@Override
		public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
			spanContextMap.remove(spanWrapper);
		}
	}
}
