package org.stagemonitor.tracing.reporter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.stagemonitor.core.util.InetAddresses;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.util.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.opentracing.tag.Tags;

import static org.stagemonitor.tracing.utils.SpanUtils.IPV4_STRING;
import static org.stagemonitor.tracing.utils.SpanUtils.PARAMETERS_PREFIX;

public class SpanJsonModule extends JsonUtils.StagemonitorJacksonModule {

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
				Map<String, Object> parameters = null;
				for (Map.Entry<String, Object> tag : span.getTags().entrySet()) {
					if (tag.getKey().startsWith(SpanWrapper.INTERNAL_TAG_PREFIX)) {
						continue;
					}
					if (tag.getKey().startsWith(PARAMETERS_PREFIX)) {
						if (parameters == null) {
							parameters = new HashMap<String, Object>();
						}
						parameters.put(tag.getKey().replace(PARAMETERS_PREFIX, ""), tag.getValue());
					} else if (tag.getKey().equals(Tags.PEER_HOST_IPV4.getKey()) && tag.getValue() instanceof Integer) {
						gen.writeStringField(IPV4_STRING, InetAddresses.fromInteger((Integer) tag.getValue()).getHostAddress());
					} else {
						gen.writeObjectField(tag.getKey(), tag.getValue());
					}
				}
				if (parameters != null && !parameters.isEmpty()) {
					gen.writeArrayFieldStart("parameters");
					for (Map.Entry<String, Object> entry : parameters.entrySet()) {
						gen.writeStartObject();
						gen.writeObjectField("key", entry.getKey());
						gen.writeObjectField("value", entry.getValue());
						gen.writeEndObject();
					}
					gen.writeEndArray();
				}

				// always include error tag so we can have a successful/error filter in Kibana
				if (!span.getTags().containsKey(Tags.ERROR.getKey())) {
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
}
