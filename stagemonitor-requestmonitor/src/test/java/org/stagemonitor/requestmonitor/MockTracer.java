package org.stagemonitor.requestmonitor;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.requestmonitor.tracing.NoopSpan;

import java.io.IOException;
import java.util.Collections;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class MockTracer implements Tracer {

	static {
		JsonUtils.getMapper().registerModule(new Module() {
			@Override
			public String getModuleName() {
				return "stagemonitor-noop-spans";
			}

			@Override
			public Version version() {
				return new Version(1, 0, 0, "", "org.stagemonitor", "stagemonitor-requestmonitor");
			}

			@Override
			public void setupModule(final SetupContext context) {
				context.addSerializers(new SimpleSerializers(Collections.<JsonSerializer<?>>singletonList(new StdSerializer<NoopSpan>(NoopSpan.class) {
					@Override
					public void serialize(NoopSpan value, JsonGenerator gen, SerializerProvider provider) throws IOException {
					}
				})));
			}
		});
	}

	@Override
	public SpanBuilder buildSpan(String operationName) {
		final SpanBuilder spanBuilder = mock(SpanBuilder.class);
		final Span mockSpan = spy(NoopSpan.INSTANCE);
		when(spanBuilder.start()).thenReturn(mockSpan);
		when(spanBuilder.asChildOf(any(SpanContext.class))).thenReturn(spanBuilder);
		when(spanBuilder.asChildOf(any(Span.class))).thenReturn(spanBuilder);
		when(spanBuilder.withStartTimestamp(anyLong())).thenReturn(spanBuilder);
		when(spanBuilder.withTag(any(), anyString()))
				.then(invocation -> {
					mockSpan.setTag(invocation.getArgument(0), invocation.<String>getArgument(1));
					return spanBuilder;
				});
		when(spanBuilder.withTag(any(), any(Number.class)))
				.then(invocation -> {
					mockSpan.setTag(invocation.getArgument(0), invocation.<Number>getArgument(1));
					return spanBuilder;
				});
		when(spanBuilder.withTag(any(), anyBoolean()))
				.then(invocation -> {
					mockSpan.setTag(invocation.getArgument(0), invocation.<Boolean>getArgument(1));
					return spanBuilder;
				});
		return spanBuilder;
	}

	@Override
	public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
	}

	@Override
	public <C> SpanContext extract(Format<C> format, C carrier) {
		return mock(SpanContext.class);
	}
}
