package org.stagemonitor.tracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

public class ExternalHttpRequest extends AbstractExternalRequest {

	private final String method;
	private final String url;
	private final String host;
	private final int port;

	public ExternalHttpRequest(Tracer tracer, String method, String url, String host, int port) {
		super(tracer, method + " " + url);
		this.method = method;
		this.url = url;
		this.host = host;
		this.port = port;
	}

	@Override
	public Span createSpan() {
		final Span span = super.createSpan();
		Tags.HTTP_URL.set(span, url);
		Tags.PEER_HOSTNAME.set(span, host);
		Tags.PEER_PORT.set(span, port);
		span.setTag(AbstractExternalRequest.EXTERNAL_REQUEST_METHOD, method);
		return span;
	}

	@Override
	protected String getType() {
		return "outgoing-http";
	}
}
