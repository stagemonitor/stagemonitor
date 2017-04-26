package org.stagemonitor.tracing.sampling;

import org.junit.Test;
import org.stagemonitor.tracing.SpanContextInformation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class PostExecutionInterceptorContextTest {

	private PostExecutionInterceptorContext interceptorContext = new PostExecutionInterceptorContext(mock(SpanContextInformation.class));

	@Test
	public void excludeCallTree() throws Exception {
		assertFalse(interceptorContext.isExcludeCallTree());

		interceptorContext.excludeCallTree("reasons");

		assertTrue(interceptorContext.isExcludeCallTree());
	}

	@Test
	public void mustPreserveCallTree() throws Exception {
		interceptorContext.excludeCallTree("reasons");
		interceptorContext.mustPreserveCallTree("reasons");
		assertFalse(interceptorContext.isExcludeCallTree());

		interceptorContext.excludeCallTree("reasons");

		assertFalse(interceptorContext.isExcludeCallTree());
	}

}
