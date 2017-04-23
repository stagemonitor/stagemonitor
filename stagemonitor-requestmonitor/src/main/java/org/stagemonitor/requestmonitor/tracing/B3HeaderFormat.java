package org.stagemonitor.requestmonitor.tracing;

import java.util.Iterator;
import java.util.Map;

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
