package org.stagemonitor.web.servlet.eum;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.SpanContextInformation.SpanContextSpanEventListener;
import org.stagemonitor.tracing.SpanContextInformation.SpanFinalizer;
import org.stagemonitor.tracing.wrapper.SpanWrappingTracer;

import java.util.HashMap;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ClientSpanLongTagProcessorTest {

	private static final String REQUEST_PARAMETER_NAME = "param";
	private static final String TAG = "tag";
	private MockTracer mockTracer;
	private Tracer.SpanBuilder spanBuilder;
	private HashMap<String, String[]> servletRequestParameters = new HashMap<>();

	@Test
	public void testProcessSpan_withValidLongValueReturnsThatValue() throws Exception {
		// Given
		addServletParameter("123");
		ClientSpanLongTagProcessor clientSpanLongTagProcessor = new ClientSpanLongTagProcessor(null, TAG, REQUEST_PARAMETER_NAME);

		// When
		SpanContextInformation context = runProcessor(clientSpanLongTagProcessor);

		// Then
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = mockTracer.finishedSpans().get(0);
		assertThat(mockSpan.tags().get(TAG)).isEqualTo(123L);
		assertThat(context.isSampled()).isTrue();
	}

	private void addServletParameter(String parameterValue) {
		servletRequestParameters.put(REQUEST_PARAMETER_NAME, new String[]{parameterValue});
	}

	@Test
	public void testProcessSpan_withInvalidValueReturnsNull() throws Exception {
		// Given
		addServletParameter("invalid");
		ClientSpanLongTagProcessor clientSpanLongTagProcessor = new ClientSpanLongTagProcessor(null, TAG, REQUEST_PARAMETER_NAME);

		// When
		SpanContextInformation context = runProcessor(clientSpanLongTagProcessor);

		// Then
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = mockTracer.finishedSpans().get(0);
		assertThat(mockSpan.tags()).doesNotContainKey(TAG);
		assertThat(context.isSampled()).isTrue();
	}

	@Test
	public void testProcessSpan_withUpperIgnoresIfGreater()  throws Exception {
		// Given
		addServletParameter("6");
		ClientSpanLongTagProcessor clientSpanLongTagProcessor = new ClientSpanLongTagProcessor(null, TAG, REQUEST_PARAMETER_NAME)
				.upperBound(5);

		// When
		SpanContextInformation context = runProcessor(clientSpanLongTagProcessor);

		// Then
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = mockTracer.finishedSpans().get(0);
		assertThat(mockSpan.tags()).doesNotContainKeys(TAG);
		assertThat(context.isSampled()).isTrue();
	}

	@Test
	public void testProcessSpan_withUpperBoundDiscardsIfGreater()  throws Exception {
		// Given
		addServletParameter("6");
		ClientSpanLongTagProcessor clientSpanLongTagProcessor = new ClientSpanLongTagProcessor(null, TAG, REQUEST_PARAMETER_NAME)
				.upperBound(5)
				.discardSpanOnBoundViolation(true);

		// When
		SpanContextInformation context = runProcessor(clientSpanLongTagProcessor);

		// Then
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = mockTracer.finishedSpans().get(0);
		assertThat(mockSpan.tags()).doesNotContainKeys(TAG);
		assertThat(context.isSampled()).isFalse();
	}

	@Test
	public void testProcessSpan_withUpperBoundAllowingIfEquals()  throws Exception {
		// Given
		addServletParameter("6");
		ClientSpanLongTagProcessor clientSpanLongTagProcessor = new ClientSpanLongTagProcessor(null, TAG, REQUEST_PARAMETER_NAME)
				.upperBound(6)
				.discardSpanOnBoundViolation(true);

		// When
		SpanContextInformation context = runProcessor(clientSpanLongTagProcessor);

		// Then
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = mockTracer.finishedSpans().get(0);
		assertThat(mockSpan.tags().get(TAG)).isEqualTo(6L);
		assertThat(context.isSampled()).isTrue();
	}

	@Test
	public void testProcessSpan_withLowerBoundIgnoresIfLower()  throws Exception {
		// Given
		addServletParameter("4");
		ClientSpanLongTagProcessor clientSpanLongTagProcessor = new ClientSpanLongTagProcessor(null, TAG, REQUEST_PARAMETER_NAME)
				.lowerBound(5);

		// When
		SpanContextInformation context = runProcessor(clientSpanLongTagProcessor);

		// Then
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = mockTracer.finishedSpans().get(0);
		assertThat(mockSpan.tags()).doesNotContainKeys(TAG);
		assertThat(context.isSampled()).isTrue();
	}

	@Test
	public void testProcessSpan_withLowerBoundDiscardsIfLower()  throws Exception {
		// Given
		addServletParameter("4");
		ClientSpanLongTagProcessor clientSpanLongTagProcessor = new ClientSpanLongTagProcessor(null, TAG, REQUEST_PARAMETER_NAME)
				.lowerBound(5)
				.discardSpanOnBoundViolation(true);

		// When
		SpanContextInformation context = runProcessor(clientSpanLongTagProcessor);

		// Then
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = mockTracer.finishedSpans().get(0);
		assertThat(mockSpan.tags()).doesNotContainKeys(TAG);
		assertThat(context.isSampled()).isFalse();
	}

	@Test
	public void testProcessSpan_withLowerBoundAllowingIfEquals()  throws Exception {
		// Given
		addServletParameter("5");
		ClientSpanLongTagProcessor clientSpanLongTagProcessor = new ClientSpanLongTagProcessor(null, TAG, REQUEST_PARAMETER_NAME)
				.lowerBound(5)
				.discardSpanOnBoundViolation(true);

		// When
		SpanContextInformation context = runProcessor(clientSpanLongTagProcessor);

		// Then
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = mockTracer.finishedSpans().get(0);
		assertThat(mockSpan.tags().get(TAG)).isEqualTo(5L);
		assertThat(context.isSampled()).isTrue();
	}

	private SpanContextInformation runProcessor(ClientSpanLongTagProcessor clientSpanLongTagProcessor) {
		clientSpanLongTagProcessor.processSpanBuilderImpl(spanBuilder, servletRequestParameters);
		final Span span = spanBuilder.start();
		clientSpanLongTagProcessor.processSpanImpl(span, servletRequestParameters);
		final SpanContextInformation spanContextInformation = SpanContextInformation.forSpan(span);
		span.finish();
		return spanContextInformation;
	}

	@Before
	public void setUp() {
		this.mockTracer = new MockTracer();
		SpanWrappingTracer spanWrappingTracer = new SpanWrappingTracer(mockTracer, asList(new SpanContextSpanEventListener(), new SpanFinalizer()));
		this.spanBuilder = spanWrappingTracer.buildSpan("");
		this.servletRequestParameters = new HashMap<>();
	}
}
