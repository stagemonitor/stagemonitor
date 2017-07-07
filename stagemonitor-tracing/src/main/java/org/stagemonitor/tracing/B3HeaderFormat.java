package org.stagemonitor.tracing;

import java.util.Iterator;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;

/**
 * A {@link io.opentracing.Tracer} is only compatible with stagemonitor if it supports the B3 header format
 *
 * @see <a href="https://github.com/openzipkin/b3-propagation">https://github.com/openzipkin/b3-propagation</a>
 */
public final class B3HeaderFormat implements Format<TextMap> {
	public static final String TRACE_ID_NAME = "X-B3-TraceId";
	public static final String SPAN_ID_NAME = "X-B3-SpanId";
	public static final String PARENT_SPAN_ID_NAME = "X-B3-ParentSpanId";

	public static final B3HeaderFormat INSTANCE = new B3HeaderFormat();

	private B3HeaderFormat() {
	}

	public static class B3Identifiers {
		private String traceId;
		private String spanId;
		private String parentSpanId;

		public String getTraceId() {
			return traceId;
		}

		void setTraceId(String traceId) {
			this.traceId = traceId;
		}

		public String getSpanId() {
			return spanId;
		}

		void setSpanId(String spanId) {
			this.spanId = spanId;
		}

		public String getParentSpanId() {
			return parentSpanId;
		}

		void setParentSpanId(String parentSpanId) {
			this.parentSpanId = parentSpanId;
		}
	}

	public static String getSpanId(Tracer tracer, final Span span) {
		final String[] spanId = new String[1];
		tracer.inject(span.context(), B3HeaderFormat.INSTANCE, new B3HeaderFormat.B3InjectAdapter() {
			@Override
			public void setParentId(String value) {
			}

			@Override
			public void setSpanId(String value) {
				spanId[0] = value;
			}

			@Override
			public void setTraceId(String value) {
			}
		});
		return spanId[0];
	}

	public static B3Identifiers getB3Identifiers(Tracer tracer, final Span span) {
		final B3Identifiers b3Identifiers = new B3Identifiers();
		tracer.inject(span.context(), B3HeaderFormat.INSTANCE, new B3HeaderFormat.B3InjectAdapter() {
			@Override
			public void setParentId(String value) {
				b3Identifiers.setParentSpanId(value);
			}

			@Override
			public void setSpanId(String value) {
				b3Identifiers.setSpanId(value);
			}

			@Override
			public void setTraceId(String value) {
				b3Identifiers.setTraceId(value);
			}
		});
		return b3Identifiers;
	}

	public static abstract class B3InjectAdapter implements TextMap {

		@Override
		public Iterator<Map.Entry<String, String>> iterator() {
			throw new UnsupportedOperationException("B3InjectAdapter should only be used with Tracer.inject()");
		}

		@Override
		public void put(String key, String value) {
			if (TRACE_ID_NAME.equalsIgnoreCase(key.toLowerCase())) {
				setTraceId(value);
			} else if (SPAN_ID_NAME.equalsIgnoreCase(key.toLowerCase())) {
				setSpanId(value);
			} else if (PARENT_SPAN_ID_NAME.equalsIgnoreCase(key.toLowerCase())) {
				setParentId(value);
			}
		}

		public abstract void setTraceId(String value);

		public abstract void setSpanId(String value);

		public abstract void setParentId(String value);
	}
}
