package org.stagemonitor.tracing;

import org.junit.Test;
import org.springframework.web.util.NestedServletException;
import org.stagemonitor.tracing.utils.SpanUtils;

import io.opentracing.Span;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TestUnnestException {

	@Test
	public void testUnnestNestedServletException() throws Exception {
		final TracingPlugin tracingPlugin = new TracingPlugin();
		final Span span = mock(Span.class);

		SpanUtils.setException(span, new NestedServletException("Eat this!", new RuntimeException("bazinga!")), tracingPlugin.getIgnoreExceptions(), tracingPlugin.getUnnestExceptions());

		verify(span).setTag("exception.class", "java.lang.RuntimeException");
		verify(span).setTag("exception.message", "bazinga!");
	}

}
