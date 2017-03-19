package org.stagemonitor.requestmonitor.tracing.jaeger;


import com.uber.jaeger.LogData;
import com.uber.jaeger.Span;
import com.uber.jaeger.reporters.Reporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import java.util.List;
import java.util.Map;

public class LoggingSpanReporter implements Reporter {

	private static final Logger logger = LoggerFactory.getLogger(LoggingSpanReporter.class);
	private final RequestMonitorPlugin requestMonitorPlugin;

	public LoggingSpanReporter(RequestMonitorPlugin requestMonitorPlugin) {
		this.requestMonitorPlugin = requestMonitorPlugin;
	}

	@Override
	public void report(Span span) {
		if (!requestMonitorPlugin.isLogSpans()) {
			return;
		}
		logger.info("Reporting span");
		StringBuilder sb = new StringBuilder();
		sb.append("\n###########################\n");
		sb.append("# Span report             #\n");
		sb.append("###########################\n");
		appendLine(sb, "name", span.getOperationName());
		appendLine(sb, "duration", span.getDuration());
		appendLine(sb, "context", span.context().contextAsString());
		appendLine(sb, "endpoint", span.getPeer());
		sb.append("###########################\n");
		sb.append("# Tags                    #\n");
		sb.append("###########################\n");
		for (Map.Entry<String, Object> entry : span.getTags().entrySet()) {
			if (!SpanUtils.CALL_TREE_JSON.equals(entry.getKey())) {
				appendLine(sb, entry.getKey(), entry.getValue());
			}
		}
		sb.append("###########################\n");
		final List<LogData> logs = span.getLogs();
		if (logs != null) {
			sb.append("###########################\n");
			sb.append("# Logs                    #\n");
			sb.append("###########################\n");
			for (LogData logData : logs) {
				appendLine(sb, logData.getTime(), logData.getMessage());
				appendLine(sb, "payload", logData.getPayload());
			}
			sb.append("###########################\n");
		}
		logger.info(sb.toString());
	}

	private void appendLine(StringBuilder sb, Object key, Object value) {
		if (value != null) {
			sb.append("# ").append(key).append(": ").append(value).append('\n');
		}
	}

	@Override
	public void close() {
	}
}
