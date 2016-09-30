package org.stagemonitor.web.opentracing;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;

/**
 * A TextMap carrier for {@link HttpServletResponse} for use with Tracer.inject() ONLY (it has no read methods).
 *
 * @see Tracer#inject(SpanContext, Format, Object)
 */
public class HttpServletResponseTextMapInjectAdapter implements TextMap {

	private final HttpServletResponse httpServletResponse;

	public HttpServletResponseTextMapInjectAdapter(HttpServletResponse httpServletResponse) {
		this.httpServletResponse = httpServletResponse;
	}

	@Override
	public Iterator<Map.Entry<String, String>> iterator() {
		throw new UnsupportedOperationException("TextMapInjectAdapter should only be used with Tracer.inject()");
	}

	@Override
	public void put(String key, String value) {
		httpServletResponse.addHeader(key, value);
	}
}
