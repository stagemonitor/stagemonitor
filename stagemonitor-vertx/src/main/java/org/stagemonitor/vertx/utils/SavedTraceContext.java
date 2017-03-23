package org.stagemonitor.vertx.utils;

import com.uber.jaeger.context.TraceContext;
import io.opentracing.Span;

/**
 * Created by Glamarre on 3/20/2017.
 */
public class SavedTraceContext {
	private final TraceContext context;
	private final Span currentSpan;

	public SavedTraceContext(TraceContext context) {
		this.context = context;
		this.currentSpan = context.isEmpty() ? null : context.getCurrentSpan();
	}

	public TraceContext getTraceContext() {
		return context;
	}

	public Span getCurrentSpan() {
		return currentSpan;
	}
}
