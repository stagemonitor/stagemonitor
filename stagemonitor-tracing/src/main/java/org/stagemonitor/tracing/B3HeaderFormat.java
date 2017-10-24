package org.stagemonitor.tracing;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.util.GlobalTracer;

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
		private final String traceId;
		private final String spanId;
		private final String parentSpanId;

		public B3Identifiers(String traceId, String spanId, String parentSpanId) {
			this.traceId = traceId;
			this.spanId = spanId;
			this.parentSpanId = parentSpanId;
		}

		public static Builder builder() {
			return new Builder();
		}

		public String getTraceId() {
			return traceId;
		}

		public String getSpanId() {
			return spanId;
		}

		public String getParentSpanId() {
			return parentSpanId;
		}

		public static class Builder {

			private String traceId;
			private String spanId;
			private String parentSpanId;

			Builder() {
			}

			public Builder traceId(String traceId) {
				this.traceId = traceId;
				return this;
			}

			public Builder spanId(String spanId) {
				this.spanId = spanId;
				return this;
			}

			public Builder parentSpanId(String parentSpanId) {
				this.parentSpanId = parentSpanId;
				return this;
			}

			public B3HeaderFormat.B3Identifiers build() {
				return new B3HeaderFormat.B3Identifiers(traceId, spanId, parentSpanId);
			}
		}

		@Override
		public String toString() {
			final String s = traceId + ':' + spanId;
			if (parentSpanId != null) {
				return s + ':' + parentSpanId;
			} else {
				return s;
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			B3Identifiers that = (B3Identifiers) o;

			if (traceId != null ? !traceId.equals(that.traceId) : that.traceId != null) return false;
			if (spanId != null ? !spanId.equals(that.spanId) : that.spanId != null) return false;
			return parentSpanId != null ? parentSpanId.equals(that.parentSpanId) : that.parentSpanId == null;
		}

		@Override
		public int hashCode() {
			int result = traceId != null ? traceId.hashCode() : 0;
			result = 31 * result + (spanId != null ? spanId.hashCode() : 0);
			result = 31 * result + (parentSpanId != null ? parentSpanId.hashCode() : 0);
			return result;
		}
	}

	public static String getTraceId(final Span span) {
		return getTraceId(GlobalTracer.get(), span);
	}

	public static String getTraceId(Tracer tracer, final Span span) {
		final String[] spanId = new String[1];
		tracer.inject(span.context(), B3HeaderFormat.INSTANCE, new B3HeaderFormat.B3InjectAdapter() {
			@Override
			public void setParentId(String value) {
			}

			@Override
			public void setSpanId(String value) {
			}

			@Override
			public void setTraceId(String value) {
				spanId[0] = value;
			}
		});
		return spanId[0];
	}

	public static boolean isRoot(Tracer tracer, final Span span) {
		final AtomicBoolean root = new AtomicBoolean(true);
		tracer.inject(span.context(), B3HeaderFormat.INSTANCE, new B3HeaderFormat.B3InjectAdapter() {
			@Override
			public void setParentId(String value) {
				root.set(false);
			}

			@Override
			public void setSpanId(String value) {
			}

			@Override
			public void setTraceId(String value) {
			}
		});
		return root.get();
	}

	public static B3Identifiers getB3Identifiers(final Span span) {
		return getB3Identifiers(GlobalTracer.get(), span);
	}

	public static B3Identifiers getB3Identifiers(Tracer tracer, final Span span) {
		final B3Identifiers.Builder b3Identifiers = B3Identifiers.builder();
		tracer.inject(span.context(), B3HeaderFormat.INSTANCE, new B3HeaderFormat.B3InjectAdapter() {
			@Override
			public void setParentId(String value) {
				b3Identifiers.parentSpanId(value);
			}

			@Override
			public void setSpanId(String value) {
				b3Identifiers.spanId(value);
			}

			@Override
			public void setTraceId(String value) {
				b3Identifiers.traceId(value);
			}
		});
		return b3Identifiers.build();
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
