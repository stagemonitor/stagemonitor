package org.stagemonitor.web.servlet.eum;

import org.junit.Test;

import java.util.HashMap;

import io.opentracing.mock.MockTracer;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientSpanLongTagProcessorTest {

	public static final String REQUEST_PARAMETER_NAME = "param";
	public static final String TAG = "tag";

	@Test
	public void testProcessSpanBuilderImpl_withValidLongValueReturnsThatValue() throws Exception {
		// Given
		MockTracer mockTracer = new MockTracer();
		final MockTracer.SpanBuilder spanBuilder = mockTracer.buildSpan("");
		final HashMap<String, String[]> servletRequestParameters = new HashMap<>();
		servletRequestParameters.put(REQUEST_PARAMETER_NAME, new String[]{"123"});
		ClientSpanLongTagProcessor clientSpanLongTagProcessor = new ClientSpanLongTagProcessor(null, TAG, REQUEST_PARAMETER_NAME);

		// When
		clientSpanLongTagProcessor.processSpanBuilderImpl(spanBuilder, servletRequestParameters);
		spanBuilder.start().finish();

		// Then
		assertThat(mockTracer.finishedSpans().get(0).tags().get(TAG)).isEqualTo(123L);
	}

	@Test
	public void testProcessSpanBuilderImpl_withInvalidValueReturnsNull() throws Exception {
		// Given
		MockTracer mockTracer = new MockTracer();
		final MockTracer.SpanBuilder spanBuilder = mockTracer.buildSpan("");
		final HashMap<String, String[]> servletRequestParameters = new HashMap<>();
		servletRequestParameters.put(REQUEST_PARAMETER_NAME, new String[]{"invalid"});
		ClientSpanLongTagProcessor clientSpanLongTagProcessor = new ClientSpanLongTagProcessor(null, TAG, REQUEST_PARAMETER_NAME);

		// When
		clientSpanLongTagProcessor.processSpanBuilderImpl(spanBuilder, servletRequestParameters);
		spanBuilder.start().finish();

		// Then
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		assertThat(mockTracer.finishedSpans().get(0).tags().get(TAG)).isNull();

	}

}
