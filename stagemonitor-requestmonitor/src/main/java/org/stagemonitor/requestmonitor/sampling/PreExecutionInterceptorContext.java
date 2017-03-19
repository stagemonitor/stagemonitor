package org.stagemonitor.requestmonitor.sampling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.requestmonitor.SpanContextInformation;

public class PreExecutionInterceptorContext extends AbstractInterceptorContext<PreExecutionInterceptorContext> {

	private static final Logger logger = LoggerFactory.getLogger(PreExecutionInterceptorContext.class);

	private boolean mustCollectCallTree = false;
	private boolean collectCallTree = true;

	public PreExecutionInterceptorContext(SpanContextInformation spanContext) {
		super(spanContext);
	}

	/**
	 * Makes sure, that a call tree is collected for the current request. In other words, profiles this request.
	 *
	 * @param reason the reason why a call tree should always be collected (debug message)
	 * @return <code>this</code> for chaining
	 */
	public PreExecutionInterceptorContext mustCollectCallTree(String reason) {
		logger.debug("Must collect call tree because {}", reason);
		mustCollectCallTree = true;
		collectCallTree = true;
		return this;
	}

	/**
	 * Requests that this request should not be profiled
	 *
	 * @param reason the reason why no call tree should be collected (debug message)
	 * @return <code>this</code> for chaining
	 */
	public PreExecutionInterceptorContext shouldNotCollectCallTree(String reason) {
		if (!mustCollectCallTree) {
			logger.debug("Should not collect call tree because {}", reason);
			collectCallTree = false;
		}
		return this;
	}

	public boolean isCollectCallTree() {
		return collectCallTree;
	}
}
