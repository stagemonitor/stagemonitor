package org.stagemonitor.tracing.soap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import io.opentracing.propagation.TextMap;

class SOAPMessageInjectAdapter implements TextMap {
	private final Map<String, List<String>> headers;

	SOAPMessageInjectAdapter(SOAPMessageContext context) {
		this.headers = initHeaderMap(context);
	}

	private Map<String, List<String>> initHeaderMap(SOAPMessageContext context) {
		@SuppressWarnings("unchecked")
		Map<String, List<String>> requestHeaders = (Map<String, List<String>>) context.get(MessageContext.HTTP_REQUEST_HEADERS);
		if (requestHeaders == null) {
			requestHeaders = new HashMap<String, List<String>>();
			context.put(MessageContext.HTTP_REQUEST_HEADERS, requestHeaders);
		}
		return requestHeaders;
	}

	@Override
	public Iterator<Map.Entry<String, String>> iterator() {
		throw new UnsupportedOperationException("SOAPMessageInjectAdapter should only be used with Tracer.inject()");
	}

	@Override
	public void put(String key, String value) {
		headers.put(key, Collections.singletonList(value));
	}

}
