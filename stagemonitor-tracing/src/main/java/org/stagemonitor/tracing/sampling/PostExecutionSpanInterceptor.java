package org.stagemonitor.tracing.sampling;

import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.StagemonitorSPI;

/**
 * Allows implementers to customize or omit reporting a {@link io.opentracing.Span}
 * <p/>
 * The post interceptor is executed after the operation has been completed and right before the span should be reported.
 * <p/>
 * To add an interceptor, call <code>Stagemonitor.getPlugin(TracingPlugin.class).registerPostInterceptor(PostExecutionSpanInterceptor)</code>
 * or place a file under <code>META-INF/services/org.stagemonitor.tracing.sampling.PostExecutionSpanInterceptor</code>
 * and insert the canonical class name of your implementation.
 */
public abstract class PostExecutionSpanInterceptor implements StagemonitorSPI {

	public void init(ConfigurationRegistry configuration) {
	}

	/**
	 * This method is called before a span gets reported.
	 * <p/>
	 * The implementer of this method can decide whether or not to report the span or to exclude the call tree.
	 *
	 * @param context contextual information about the current report that is about to happen
	 */
	public abstract void interceptReport(PostExecutionInterceptorContext context);
}
