package org.stagemonitor.tracing.sampling;

import org.junit.Test;
import org.stagemonitor.tracing.SpanContextInformation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class PreExecutionInterceptorContextTest {

	private PreExecutionInterceptorContext interceptorContext = new PreExecutionInterceptorContext(mock(SpanContextInformation.class));

	@Test
	public void mustCollectCallTree() throws Exception {
		interceptorContext.shouldNotCollectCallTree("reasons");
		assertFalse(interceptorContext.isCollectCallTree());

		interceptorContext.mustCollectCallTree("reasons");
		assertTrue(interceptorContext.isCollectCallTree());

		interceptorContext.shouldNotCollectCallTree("reasons");
		assertTrue(interceptorContext.isCollectCallTree());
	}

	@Test
	public void shouldNotCollectCallTree() throws Exception {
		assertTrue(interceptorContext.isCollectCallTree());

		interceptorContext.shouldNotCollectCallTree("reasons");

		assertFalse(interceptorContext.isCollectCallTree());
	}

}
