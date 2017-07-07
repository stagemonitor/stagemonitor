package org.stagemonitor.tracing.reporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.tracing.B3HeaderFormat;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import java.util.Map;

public class LoggingSpanReporter extends SpanReporter {

	private static final Logger logger = LoggerFactory.getLogger(LoggingSpanReporter.class);
	private TracingPlugin tracingPlugin;

	@Override
	public void init(ConfigurationRegistry configuration) {
		this.tracingPlugin = configuration.getConfig(TracingPlugin.class);
	}

	@Override
	public void report(SpanContextInformation context, SpanWrapper spanWrapper) {
		logger.info(getLogMessage(spanWrapper));
	}

	String getLogMessage(SpanWrapper span) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n###########################\n");
		sb.append("# Span report             #\n");
		sb.append("###########################\n");
		appendLine(sb, "name", span.getOperationName());
		appendLine(sb, "duration", span.getDurationMs());
		final B3HeaderFormat.B3Identifiers b3Identifiers = B3HeaderFormat.getB3Identifiers(tracingPlugin.getTracer(), span);
		appendLine(sb, "traceId", b3Identifiers.getTraceId());
		appendLine(sb, "spanId", b3Identifiers.getSpanId());
		appendLine(sb, "parentId", b3Identifiers.getParentSpanId());
		sb.append("###########################\n");
		sb.append("# Tags                    #\n");
		sb.append("###########################\n");
		for (Map.Entry<String, Object> entry : span.getTags().entrySet()) {
			if (!SpanUtils.CALL_TREE_JSON.equals(entry.getKey())) {
				appendLine(sb, entry.getKey(), entry.getValue());
			}
		}
		sb.append("###########################\n");
		return sb.toString();
	}

	private void appendLine(StringBuilder sb, Object key, Object value) {
		if (value != null) {
			sb.append("# ").append(key).append(": ").append(value).append('\n');
		}
	}

	@Override
	public boolean isActive(SpanContextInformation spanContext) {
		return tracingPlugin.isLogSpans();
	}
}
