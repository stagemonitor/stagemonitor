package org.stagemonitor.requestmonitor.reporter;


import com.uber.jaeger.LogData;
import com.uber.jaeger.Span;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class LoggingSpanReporter extends AbstractInterceptedSpanReporter {

	private static final Logger logger = LoggerFactory.getLogger(LoggingSpanReporter.class);

	@Override
	protected void doReport(ReportArguments reportArguments, PostExecutionInterceptorContext context) {
		io.opentracing.Span span = reportArguments.getSpan();
		logger.info("Reporting span");
		if (span instanceof Span) {
			Span jaegerSpan = (Span) span;
			StringBuilder sb = new StringBuilder();
			sb.append("\n###########################\n");
			sb.append("# Span report             #\n");
			sb.append("###########################\n");
			appendLine(sb, "name", jaegerSpan.getOperationName());
			appendLine(sb, "duration", jaegerSpan.getDuration());
			appendLine(sb, "context", jaegerSpan.context().contextAsString());
			appendLine(sb, "endpoint", jaegerSpan.getPeer());
			sb.append("###########################\n");
			sb.append("# Tags                    #\n");
			sb.append("###########################\n");
			for (Map.Entry<String, Object> entry : jaegerSpan.getTags().entrySet()) {
				appendLine(sb, entry.getKey(), entry.getValue());
			}
			sb.append("###########################\n");
			final List<LogData> logs = jaegerSpan.getLogs();
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
	}

	private void appendLine(StringBuilder sb, Object key, Object value) {
		if (value != null) {
			sb.append("# ").append(key).append(": ").append(value).append('\n');
		}
	}

}
