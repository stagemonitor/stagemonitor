package org.stagemonitor.requestmonitor.reporter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.uber.jaeger.LogData;
import com.uber.jaeger.Span;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ElasticsearchSpanReporter extends AbstractInterceptedSpanReporter {

	public static final String ES_SPAN_LOGGER = "ElasticsearchSpanReporter";

	private final Logger requestTraceLogger;

	public ElasticsearchSpanReporter() {
		this(LoggerFactory.getLogger(ES_SPAN_LOGGER));
	}

	ElasticsearchSpanReporter(Logger requestTraceLogger) {
		this.requestTraceLogger = requestTraceLogger;
		JsonUtils.getMapper().registerModule(new SpanModule());
	}

	@Override
	protected void doReport(ReportArguments reportArguments, PostExecutionInterceptorContext context) {
		final String index = "stagemonitor-spans-" + StringUtils.getLogstashStyleDate();
		final String type = "spans";
		if (requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()) {
			requestTraceLogger.info(ElasticsearchClient.getBulkHeader("index", index, type) + JsonUtils.toJson(reportArguments.getSpan()));
		} else {
			if (context.getExcludedProperties().isEmpty()) {
				elasticsearchClient.index(index, type, reportArguments.getSpan());
			} else {
				elasticsearchClient.index(index, type, JsonUtils.toObjectNode(reportArguments.getSpan()).remove(context.getExcludedProperties()));
			}
		}
	}

	@Override
	public boolean isActive(IsActiveArguments isActiveArguments) {
		final boolean urlAvailable = !corePlugin.getElasticsearchUrls().isEmpty();
		final boolean logOnly = requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports();
		return (urlAvailable || logOnly) && super.isActive(isActiveArguments);
	}

	/**
	 * Add an {@link PreExecutionRequestTraceReporterInterceptor} to the interceptor list
	 *
	 * @param interceptor the interceptor that should be executed before measurement starts
	 */
	public static void registerPreInterceptor(PreExecutionRequestTraceReporterInterceptor interceptor) {
		final ElasticsearchSpanReporter thiz = getElasticsearchSpanReporter();
		if (thiz != null) {
			thiz.preInterceptors.add(interceptor);
		}
	}

	/**
	 * Add an {@link PostExecutionRequestTraceReporterInterceptor} to the interceptor list
	 *
	 * @param interceptor the interceptor that should be executed before each report
	 */
	public static void registerPostInterceptor(PostExecutionRequestTraceReporterInterceptor interceptor) {
		final ElasticsearchSpanReporter thiz = getElasticsearchSpanReporter();
		if (thiz != null) {
			thiz.postInterceptors.add(interceptor);
		}
	}

	private static ElasticsearchSpanReporter getElasticsearchSpanReporter() {
		return Stagemonitor
				.getPlugin(RequestMonitorPlugin.class)
				.getRequestMonitor()
				.getReporter(ElasticsearchSpanReporter.class);
	}

	private static class SpanModule extends Module {

		@Override
		public String getModuleName() {
			return "stagemonitor-spans";
		}

		@Override
		public Version version() {
			return new Version(1, 0, 0, "", "org.stagemonitor", "stagemonitor-requestmonitor");
		}

		@Override
		public void setupModule(SetupContext context) {
			context.addSerializers(new SimpleSerializers(Collections.<JsonSerializer<?>>singletonList(new StdSerializer<com.uber.jaeger.Span>(Span.class) {

				@Override
				public void serialize(com.uber.jaeger.Span span, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
					gen.writeStartObject();
					for (Map.Entry<String, Object> entry : span.getTags().entrySet()) {
						gen.writeObjectField(entry.getKey(), entry.getValue());
					}
					if (span.getLogs() != null) {
						gen.writeArrayFieldStart("logs");
						for (LogData logData : span.getLogs()) {
							gen.writeStartObject();
							gen.writeNumberField("time", logData.getTime());
							gen.writeStringField("message", logData.getMessage());
							gen.writeObjectField("payload", logData.getPayload());
							gen.writeEndObject();
						}
						gen.writeEndArray();
					}

					gen.writeStringField("name", span.getOperationName());
					gen.writeNumberField("duration", span.getDuration());
					gen.writeNumberField("duration_ms", TimeUnit.MICROSECONDS.toMillis(span.getDuration()));
					gen.writeNumberField("@timestamp", TimeUnit.MICROSECONDS.toMillis(span.getStart()));
					gen.writeStringField("id", StringUtils.toHexString(span.context().getSpanID()));
					gen.writeStringField("trace_id", StringUtils.toHexString(span.context().getTraceID()));
					gen.writeStringField("parent_id", StringUtils.toHexString(span.context().getParentID()));
					gen.writeBooleanField("sampled", span.context().isSampled());
					gen.writeBooleanField("debug", span.context().isDebug());
					if (span.getPeer() != null) {
						gen.writeStringField("peer.service", span.getPeer().getService_name());
						gen.writeNumberField("peer.port", span.getPeer().getPort());
						gen.writeNumberField("peer.ipv4", span.getPeer().getIpv4());
					}
					gen.writeEndObject();
				}
			})));
		}
	}

}
