package org.stagemonitor.requestmonitor.tracing;

import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.StagemonitorSPI;

import io.opentracing.Tracer;

public abstract class TracerFactory implements StagemonitorSPI {
	public abstract Tracer getTracer(StagemonitorPlugin.InitArguments initArguments);
}
