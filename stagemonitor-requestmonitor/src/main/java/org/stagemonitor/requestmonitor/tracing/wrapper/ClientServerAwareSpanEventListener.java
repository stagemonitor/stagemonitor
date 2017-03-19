package org.stagemonitor.requestmonitor.tracing.wrapper;

import io.opentracing.tag.Tags;

public class ClientServerAwareSpanEventListener extends SpanEventListener {

	protected boolean isClient;
	protected boolean isRPC;
	protected boolean isServer;

	public String onSetTag(String key, String value) {
		if (key.equals(Tags.SPAN_KIND.getKey())) {
			isClient = Tags.SPAN_KIND_CLIENT.equals(value);
			isServer = Tags.SPAN_KIND_SERVER.equals(value);
			isRPC = isClient || isServer;
		}
		return value;
	}

}
