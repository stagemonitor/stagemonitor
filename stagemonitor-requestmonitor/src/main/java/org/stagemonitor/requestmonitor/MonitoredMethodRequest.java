package org.stagemonitor.requestmonitor;

import com.uber.jaeger.context.TracingUtils;

import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.requestmonitor.utils.SpanTags;

import java.util.LinkedHashMap;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

public class MonitoredMethodRequest extends MonitoredRequest {

	private final String methodSignature;
	private final MethodExecution methodExecution;
	private final Map<String, String> safeParameters;
	private final RequestMonitorPlugin requestMonitorPlugin;

	public MonitoredMethodRequest(Configuration configuration, String methodSignature, MethodExecution methodExecution) {
		this(configuration, methodSignature, methodExecution, null);
	}

	public MonitoredMethodRequest(Configuration configuration, String methodSignature, MethodExecution methodExecution, Map<String, Object> parameters) {
		this.requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
		this.methodSignature = methodSignature;
		this.methodExecution = methodExecution;
		this.safeParameters = getSafeParameterMap(parameters);
	}

	@Override
	public String getInstanceName() {
		return null;
	}

	private Map<String, String> getSafeParameterMap(Map<String, Object> parameters) {
		if (parameters == null) {
			return null;
		}
		Map<String, String> params = new LinkedHashMap<String, String>();
		for (Map.Entry<String, Object> entry : parameters.entrySet()) {
			params.put(entry.getKey(), String.valueOf(entry.getValue()));
		}
		return RequestMonitorPlugin.getSafeParameterMap(params, requestMonitorPlugin.getConfidentialParameters());
	}

	@Override
	public Span createSpan() {
		final Tracer tracer = requestMonitorPlugin.getTracer();
		final Span span;
		if (!TracingUtils.getTraceContext().isEmpty()) {
			span = tracer.buildSpan(methodSignature).asChildOf(TracingUtils.getTraceContext().getCurrentSpan()).start();
		} else {
			span = tracer.buildSpan(methodSignature).start();
		}
		Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_SERVER);
		SpanTags.setParameters(span, safeParameters);
		SpanTags.setOperationType(span, "method_invocation");
		return span;
	}

	@Override
	public Object execute() throws Exception {
		return methodExecution.execute();
	}

	@Override
	public void onPostExecute(RequestMonitor.RequestInformation requestTrace) {
	}

	/**
	 * In a Method execution context, we only want to monitor the topmost monitored (forwarding) method call.
	 * <p/>
	 * Example:<br/>
	 * Suppose, we have three methods: monitored1(), monitored2() and notMonitored().
	 * <pre><code>
	 * public void monitored1() {
	 *     monitored2();
	 * }
	 * public void monitored2() {
	 *     notMonitored();
	 * }
	 * public void notMonitored() {}
	 * </code></pre>
	 * The first two Methods are monitored with a {@link RequestMonitor}.<br/>
	 * If method1() is called, we only want to collect metrics for method1() and not for method2().<br/>
	 * If method2() is called, we want to collect metrics for that method.<br/>
	 * If notMonitored() is called directly, we don't want to collect metrics.
	 *
	 * @return false
	 */
	@Override
	public boolean isMonitorForwardedExecutions() {
		return false;
	}

	public interface MethodExecution {
		Object execute() throws Exception;
	}
}
