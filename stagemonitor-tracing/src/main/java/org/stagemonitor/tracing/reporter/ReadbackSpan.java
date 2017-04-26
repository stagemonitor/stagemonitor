package org.stagemonitor.tracing.reporter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.util.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.opentracing.tag.Tags;

/**
 * A span which supports readback of tags and meta data
 */
public class ReadbackSpan {

	static {
		JsonUtils.getMapper().registerModule(new SpanJsonModule());
	}

	private String id;
	private String traceId;
	private String parentId;

	private String name;
	private long duration;
	private String timestamp;

	private Map<String, Object> tags = new HashMap<String, Object>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public Map<String, Object> getTags() {
		return tags;
	}

	public void setTag(String key, Object value) {
		if (value != null) {
			synchronized (this) {
				tags.put(key, value);
			}
		}
	}

	public static class SpanJsonModule extends Module {

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
			context.addSerializers(new SimpleSerializers(Collections.<JsonSerializer<?>>singletonList(new StdSerializer<ReadbackSpan>(ReadbackSpan.class) {

				@Override
				public void serialize(ReadbackSpan span, JsonGenerator gen, SerializerProvider serializers) throws IOException {
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
					// always include error tag so we can have a successful/error filter in Kibana
					if (!nestedTags.containsKey(Tags.ERROR.getKey())) {
						gen.writeBooleanField("error", false);
					}

					gen.writeStringField("name", span.getName());
					gen.writeNumberField("duration", span.getDuration());
					gen.writeNumberField("duration_ms", span.getDuration() / MICROSECONDS_OF_MILLISECOND);
					gen.writeStringField("@timestamp", span.getTimestamp());
					gen.writeStringField("id", span.getId());
					gen.writeStringField("trace_id", span.getTraceId());
					gen.writeStringField("parent_id", span.getParentId());
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
		 *
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
}
