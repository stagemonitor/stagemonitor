package org.stagemonitor.requestmonitor.tracing;

import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.StagemonitorSPI;

import io.opentracing.Tracer;

/**
 * Note that tracers have to support {@link B3HeaderFormat}
 */
public abstract class TracerFactory implements StagemonitorSPI {
	public abstract Tracer getTracer(StagemonitorPlugin.InitArguments initArguments);
}
