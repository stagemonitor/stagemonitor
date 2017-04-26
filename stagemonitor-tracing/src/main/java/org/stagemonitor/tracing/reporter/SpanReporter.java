package org.stagemonitor.tracing.reporter;

import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.StagemonitorSPI;
import org.stagemonitor.tracing.SpanContextInformation;

import io.opentracing.Span;

public abstract class SpanReporter implements StagemonitorSPI {

	public void init(ConfigurationRegistry configuration) {
	}

	/**
	 * Callback method that is called when a {@link Span} was created and is ready to be reported
	 *
	 * @param spanContext context information about the span
	 */
	public abstract void report(SpanContextInformation spanContext) throws Exception;

	/**
	 * Whether this {@link SpanReporter} is active
	 *
	 * @param spanContext context information about the span
	 * @return <code>true</code>, if this {@link SpanReporter} is active, <code>false</code> otherwise
	 */
	public abstract boolean isActive(SpanContextInformation spanContext);

}
