package org.stagemonitor.tracing.reporter;

import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.StagemonitorSPI;
import org.stagemonitor.tracing.B3HeaderFormat;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import java.util.Map;

import io.opentracing.Span;

public abstract class SpanReporter implements StagemonitorSPI {

	public void init(ConfigurationRegistry configuration) {
	}

	/**
	 * Callback method that is called when a {@link Span} was created and is ready to be reported
	 *
	 * @param spanContext context information about the span
	 * @param spanWrapper the span wrapper
	 */
	public abstract void report(SpanContextInformation spanContext, SpanWrapper spanWrapper) throws Exception;

	/**
	 * Whether this {@link SpanReporter} is active
	 *
	 * @param spanContext context information about the span
	 * @return <code>true</code>, if this {@link SpanReporter} is active, <code>false</code> otherwise
	 */
	public abstract boolean isActive(SpanContextInformation spanContext);

	public void updateSpan(B3HeaderFormat.B3Identifiers spanIdentifiers, B3HeaderFormat.B3Identifiers newSpanIdentifiers, Map<String, Object> tagsToUpdate) {
	}

}
