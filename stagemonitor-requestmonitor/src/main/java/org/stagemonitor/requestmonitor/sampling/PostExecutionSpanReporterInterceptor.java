package org.stagemonitor.requestmonitor.sampling;

import org.stagemonitor.core.StagemonitorSPI;

/**
 * Allows implementers to customize or omit reporting a {@link io.opentracing.Span}
 * <p/>
 * The post interceptor is executed after the operation has been completed and right before the span should be reported.
 * <p/>
 * To add an interceptor, call <code>Stagemonitor.getPlugin(RequestMonitorPlugin.class).registerPostInterceptor(PostExecutionSpanReporterInterceptor)</code>
 * or place a file under <code>META-INF/services/org.stagemonitor.requestmonitor.sampling.PostExecutionSpanReporterInterceptor</code>
 * and insert the canonical class name of your implementation.
 */
public abstract class PostExecutionSpanReporterInterceptor implements StagemonitorSPI {

	/**
	 * This method is called before a span gets reported.
	 * <p/>
	 * The implementer of this method can decide whether or not to report the span or to exclude the call tree.
	 *
	 * @param context contextual information about the current report that is about to happen
	 */
	public abstract void interceptReport(PostExecutionInterceptorContext context);
}
