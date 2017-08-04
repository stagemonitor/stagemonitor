package org.stagemonitor.tracing;

public interface SpanIdentifiers {
    Object getTraceId();

    Object getSpanId();
}
