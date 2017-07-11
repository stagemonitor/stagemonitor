package org.stagemonitor.tracing.reporter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.util.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import io.opentracing.tag.Tags;

public class SpanJsonModule extends JsonUtils.StagemonitorJacksonModule  {

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
		context.addSerializers(new SimpleSerializers(Collections.<JsonSerializer<?>>singletonList(new StdSerializer<SpanWrapper>(SpanWrapper.class) {

			@Override
			public void serialize(SpanWrapper span, JsonGenerator gen, SerializerProvider serializers) throws IOException {
				gen.writeStartObject();
				final Map<String, Object> nestedTags = convertDottedKeysIntoNestedObject(span.getTags());
				for (Map.Entry<String, Object> entry : nestedTags.entrySet()) {
					final Object value = entry.getValue();
					if (value != null) {
						gen.writeObjectField(entry.getKey(), value);
					}
				}
				// always include error tag so we can have a successful/error filter in Kibana
				if (!nestedTags.containsKey(Tags.ERROR.getKey())) {
					gen.writeBooleanField("error", false);
				}

				gen.writeStringField("name", span.getOperationName());
				gen.writeNumberField("duration_ms", span.getDurationMs());
				final String timestamp = StringUtils.timestampAsIsoString(span.getStartTimestampMillis());
				gen.writeStringField("@timestamp", timestamp);
				gen.writeEndObject();
			}
		})));
	}

	/**
	 * As Elasticsearch can't cope with dots in field names, we have to convert them into nested objects. That way, we
	 * can use the dotted notation in aggregations again.
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
	 *
	 * @param tags the span tags to convert
	 * @return the span tags as nested objects
	 */
	private Map<String, Object> convertDottedKeysIntoNestedObject(Map<String, Object> tags) {
		Map<String, Object> nestedTags = new HashMap<String, Object>();
		for (Map.Entry<String, Object> entry : tags.entrySet()) {
			if (entry.getKey().startsWith(SpanWrapper.INTERNAL_TAG_PREFIX)) {
				continue;
			}
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
				// starting.segments
				path = getNewNestedPath(path, pathSegment, key);
			} else {
				// last.segment
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
