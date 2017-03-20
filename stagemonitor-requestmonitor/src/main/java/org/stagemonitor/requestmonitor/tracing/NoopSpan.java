package org.stagemonitor.requestmonitor.tracing;

import java.util.Collections;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.SpanContext;

public class NoopSpan implements Span {

	public static final Span INSTANCE = new NoopSpan();

	private NoopSpan() {
	}

	@Override
	public SpanContext context() {
		return NoopSpanContext.INSTANCE;
	}

	@Override
	public void finish() {
	}

	@Override
	public void finish(long finishMicros) {
	}

	@Override
	public void close() {
	}

	@Override
	public Span setTag(String key, String value) {
		return this;
	}

	@Override
	public Span setTag(String key, boolean value) {
		return this;
	}

	@Override
	public Span setTag(String key, Number value) {
		return this;
	}

	@Override
	public Span setBaggageItem(String key, String value) {
		return this;
	}

	@Override
	public String getBaggageItem(String key) {
		return null;
	}

	@Override
	public Span setOperationName(String operationName) {
		return this;
	}

	@Override
	public Span log(String eventName, Object payload) {
		return this;
	}

	@Override
	public Span log(long timestampMicroseconds, String eventName, Object payload) {
		return this;
	}

	static final class NoopSpanContext implements SpanContext {
		static final SpanContext INSTANCE = new NoopSpanContext();

		@Override
		public Iterable<Map.Entry<String, String>> baggageItems() {
			return Collections.emptyList();
		}
	}
}
