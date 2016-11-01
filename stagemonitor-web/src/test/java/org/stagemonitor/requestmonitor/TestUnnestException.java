package org.stagemonitor.requestmonitor;

import org.junit.Test;
import org.springframework.web.util.NestedServletException;
import org.stagemonitor.requestmonitor.utils.SpanTags;

import io.opentracing.Span;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TestUnnestException {

	@Test
	public void testUnnestNestedServletException() throws Exception {
		final RequestMonitorPlugin requestMonitorPlugin = new RequestMonitorPlugin();
		final Span span = mock(Span.class);

		SpanTags.setException(span, new NestedServletException("Eat this!", new RuntimeException("bazinga!")), requestMonitorPlugin.getIgnoreExceptions(), requestMonitorPlugin.getUnnestExceptions());

		verify(span).setTag("exception.class", "java.lang.RuntimeException");
		verify(span).setTag("exception.message", "bazinga!");
	}

}
