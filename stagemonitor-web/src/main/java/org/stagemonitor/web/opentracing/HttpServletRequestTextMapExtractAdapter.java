package org.stagemonitor.web.opentracing;

import java.util.AbstractMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;

/**
 * A TextMap carrier for a {@link HttpServletRequest} for use with Tracer.extract() ONLY (it has no mutating methods).
 *
 * @see io.opentracing.Tracer#extract(Format, Object)
 */
public class HttpServletRequestTextMapExtractAdapter implements TextMap {
	private final HttpServletRequest httpServletRequest;

	public HttpServletRequestTextMapExtractAdapter(HttpServletRequest httpServletRequest) {
		this.httpServletRequest = httpServletRequest;
	}

	@Override
	public Iterator<Map.Entry<String, String>> iterator() {
		return new HttpHeaderIterator(httpServletRequest, httpServletRequest.getHeaderNames());
	}

	@Override
	public void put(String key, String value) {
		throw new UnsupportedOperationException(
				"TextMapInjectAdapter should only be used with Tracer.extract()");
	}

	private static class HttpHeaderIterator implements Iterator<Map.Entry<String, String>> {
		private final HttpServletRequest httpServletRequest;
		private final Enumeration<String> headerNames;

		private HttpHeaderIterator(HttpServletRequest httpServletRequest, Enumeration<String> headerNames) {
			this.httpServletRequest = httpServletRequest;
			this.headerNames = headerNames;
		}

		@Override
		public boolean hasNext() {
			return headerNames.hasMoreElements();
		}

		@Override
		public Map.Entry<String, String> next() {
			final String nextHeader = headerNames.nextElement();
			return new AbstractMap.SimpleEntry<String, String>(nextHeader, httpServletRequest.getHeader(nextHeader));
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove");
		}
	}
}
