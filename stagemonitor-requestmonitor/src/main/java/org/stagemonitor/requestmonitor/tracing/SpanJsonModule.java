package org.stagemonitor.requestmonitor.tracing;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.uber.jaeger.LogData;
import com.uber.jaeger.Span;

import org.stagemonitor.core.util.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SpanJsonModule extends Module {

	private static final double MICROSECONDS_OF_MILLISECOND = TimeUnit.MILLISECONDS.toMicros(1);

	@Override
	public String getModuleName() {
		return "stagemonitor-spans";
	}

	@Override
	public Version version() {
		return new Version(1, 0, 0, "", "org.stagemonitor", "stagemonitor-requestmonitor");
	}

	@Override
	public void setupModule(final SetupContext context) {
		context.addSerializers(new SimpleSerializers(Collections.<JsonSerializer<?>>singletonList(new StdSerializer<Span>(Span.class) {

			@Override
			public void serialize(Span span, JsonGenerator gen, SerializerProvider serializers) throws IOException {
				gen.writeStartObject();
				final Map<String, Object> nestedTags;
				// synchronizing on the span avoids ConcurrentModificationExceptions
				// in case other threads are for example adding tags while the span is converted to JSON
				synchronized (span) {
					nestedTags = convertDottedKeysIntoNestedObject(span.getTags());
				}
				for (Map.Entry<String, Object> entry : nestedTags.entrySet()) {
					final Object value = entry.getValue();
					if (value != null) {
						gen.writeObjectField(entry.getKey(), value);
					}
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
				gen.writeNumberField("duration_ms", span.getDuration() / MICROSECONDS_OF_MILLISECOND);
				gen.writeStringField("@timestamp", StringUtils.timestampAsIsoString(TimeUnit.MICROSECONDS.toMillis(span.getStart())));
				gen.writeStringField("id", StringUtils.toHexString(span.context().getSpanID()));
				gen.writeStringField("trace_id", StringUtils.toHexString(span.context().getTraceID()));
				gen.writeStringField("parent_id", StringUtils.toHexString(span.context().getParentID()));
				gen.writeBooleanField("sampled", span.context().isSampled());
				gen.writeBooleanField("debug", span.context().isDebug());
				if (span.getPeer() != null) {
					gen.writeObjectFieldStart("peer");
					gen.writeStringField("service", span.getPeer().getService_name());
					gen.writeNumberField("port", span.getPeer().getPort());
					gen.writeNumberField("ipv4", span.getPeer().getIpv4());
					gen.writeEndObject();
				}
				gen.writeEndObject();
			}
		})));
	}

	/**
	 * As Elasticsearch can't cope with dots in field names, we have to convert them into nested objects.
	 * That way, we can use the dotted notation in aggregations again.
	 *
	 * Example:
	 * <pre>
	 *     {
	 *         "dotted.path.foo": "bar"
	 *     }
	 *
	 *     will be converted to
	 *
	 *     {
	 *         "dotted": {
	 *             "path": {
	 *                 "foo": "bar"
	 *             }
	 *         }
	 *     }
	 * </pre>
	 * @param tags the span tags to convert
	 * @return the span tags as nested objects
	 */
	private Map<String, Object> convertDottedKeysIntoNestedObject(Map<String, Object> tags) {
		Map<String, Object> nestedTags = new HashMap<String, Object>();
		for (Map.Entry<String, Object> entry : tags.entrySet()) {
			if (entry.getKey().indexOf('.') >= 0) {
				doConvertDots(nestedTags, entry.getKey(), entry.getValue());
			} else {
				nestedTags.put(entry.getKey(), entry.getValue());
			}
		}

		return nestedTags;
	}

	private void doConvertDots(Map<String, Object> nestedTags, String key, Object value) {
		final String[] pathSegments = StringUtils.split(key, '.');
		Map<String, Object> path = nestedTags;
		for (int i = 0; i < pathSegments.length; i++) {
			String pathSegment = pathSegments[i];
			if (i + 1 < pathSegments.length) {
				path = getNewNestedPath(path, pathSegment, key);
			} else {
				// last
				path.put(pathSegment, value);
			}
		}
	}

	private Map<String, Object> getNewNestedPath(Map<String, Object> path, String pathSegment, String fullPath) {
		final Map<String, Object> newPath;
		final Object existingPath = path.get(pathSegment);
		if (existingPath != null) {
			if (existingPath instanceof Map) {
				newPath = (Map<String, Object>) existingPath;
			} else {
				throw new IllegalArgumentException("Ambiguous mapping for " + fullPath);
			}
		} else {
			newPath = new LinkedHashMap<String, Object>();
			path.put(pathSegment, newPath);
		}
		return newPath;
	}
}
