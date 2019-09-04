package org.stagemonitor.tracing;

import io.opentracing.Span;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.tracing.metrics.MetricsSpanEventListener;
import org.stagemonitor.tracing.utils.SpanUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

public class MonitoredMethodRequest extends MonitoredRequest {

	public static final String OP_TYPE_METHOD_INVOCATION = "method_invocation";
	private final String methodSignature;
	private final MethodExecution methodExecution;
	private final Map<String, String> safeParameters;
	private final TracingPlugin tracingPlugin;

	public MonitoredMethodRequest(ConfigurationRegistry configuration, String methodSignature, MethodExecution methodExecution) {
		this(configuration, methodSignature, methodExecution, null);
	}

	public MonitoredMethodRequest(ConfigurationRegistry configuration, String methodSignature, MethodExecution methodExecution, Map<String, Object> parameters) {
		this.tracingPlugin = configuration.getConfig(TracingPlugin.class);
		this.methodSignature = methodSignature;
		this.methodExecution = methodExecution;
		this.safeParameters = getSafeParameterMap(parameters);
	}

	private Map<String, String> getSafeParameterMap(Map<String, Object> parameters) {
		if (parameters == null) {
			return null;
		}
		Map<String, String> params = new LinkedHashMap<String, String>();
		for (Map.Entry<String, Object> entry : parameters.entrySet()) {
			String valueAsString;
			try {
				valueAsString = String.valueOf(entry.getValue());
			}
			catch (Exception e) {
				valueAsString = "[unavailable (" + e.getMessage() + ")]";
			}
			params.put(entry.getKey(), valueAsString);
		}
		return TracingPlugin.getSafeParameterMap(params, tracingPlugin.getConfidentialParameters());
	}

	@Override
	public Span createSpan() {
		final Tracer tracer = tracingPlugin.getTracer();
		final Tracer.SpanBuilder spanBuilder = tracer.buildSpan(methodSignature)
					.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
		final Span span = spanBuilder
				.withTag(SpanUtils.OPERATION_TYPE, OP_TYPE_METHOD_INVOCATION)
				.withTag(MetricsSpanEventListener.ENABLE_TRACKING_METRICS_TAG, true)
				.start();
		SpanUtils.setParameters(span, safeParameters);
		return span;
	}

	@Override
	public void execute() throws Exception {
		methodExecution.execute();
	}

	public interface MethodExecution {
		void execute() throws Exception;
	}
}
