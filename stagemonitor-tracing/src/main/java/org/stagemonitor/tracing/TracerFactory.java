package org.stagemonitor.tracing;

import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.StagemonitorSPI;

import io.opentracing.Span;
import io.opentracing.Tracer;

/**
 * Note that tracers have to support {@link B3HeaderFormat}
 */
public abstract class TracerFactory implements StagemonitorSPI {
	public abstract Tracer getTracer(StagemonitorPlugin.InitArguments initArguments);

	/**
	 * @deprecated remove once https://github.com/opentracing/specification/issues/91 is resolved
	 */
	@Deprecated
	public abstract boolean isRoot(Span span);

	/**
	 * @deprecated remove once https://github.com/opentracing/specification/issues/92 is resolved
	 */
	@Deprecated
	public abstract boolean isSampled(Span span);
}
