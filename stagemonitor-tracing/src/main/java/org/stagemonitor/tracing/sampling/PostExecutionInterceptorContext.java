package org.stagemonitor.tracing.sampling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.tracing.SpanContextInformation;

import io.opentracing.Span;

public class PostExecutionInterceptorContext extends AbstractInterceptorContext<PreExecutionInterceptorContext> {

	private static final Logger logger = LoggerFactory.getLogger(PostExecutionInterceptorContext.class);

	private boolean mustPreserveCallTree = false;
	private boolean excludeCallTree = false;

	public PostExecutionInterceptorContext(SpanContextInformation spanContext) {
		super(spanContext);
	}

	/**
	 * Requests that the call tree should not be added to the current {@link Span}
	 *
	 * @param reason the reason why the call tree should be excluded (debug message)
	 * @return <code>this</code> for chaining
	 */
	public PostExecutionInterceptorContext excludeCallTree(String reason) {
		if (!mustPreserveCallTree) {
			logger.debug("Excluding call tree because {}", reason);
			excludeCallTree = true;
		}
		return this;
	}

	/**
	 * Makes sure that the call tree can't be {@linkplain #excludeCallTree excluded}
	 * <p/>
	 * Note: if the call tree has not been collected, calling this method won't restore it.
	 * See also {@link PreExecutionInterceptorContext#mustCollectCallTree}
	 *
	 * @param reason the reason why the call tree should always be preserved (debug message)
	 * @return <code>this</code> for chaining
	 */
	public PostExecutionInterceptorContext mustPreserveCallTree(String reason) {
		if (getSpanContext().getCallTree() == null) {
			logger.info("Can't preserve the call tree because it has not been collected");
		}
		logger.debug("Must preserve call tree because {}", reason);
		mustPreserveCallTree = true;
		excludeCallTree = false;
		return this;
	}

	public boolean isExcludeCallTree() {
		return excludeCallTree;
	}
}
