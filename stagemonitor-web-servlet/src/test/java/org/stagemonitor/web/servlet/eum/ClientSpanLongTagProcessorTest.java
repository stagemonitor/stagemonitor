package org.stagemonitor.web.servlet.eum;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.tracing.SpanContextInformation.SpanContextSpanEventListener;
import org.stagemonitor.tracing.SpanContextInformation.SpanFinalizer;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.tracing.wrapper.SpanWrappingTracer;

import java.util.HashMap;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.stagemonitor.web.servlet.eum.ClientSpanTagProcessor.TYPE_ALL;

public class ClientSpanLongTagProcessorTest {

	private static final String REQUEST_PARAMETER_NAME = "param";
	private static final String TAG = "tag";
	private MockTracer mockTracer;
	private SpanWrappingTracer.SpanWrappingSpanBuilder spanBuilder;
	private HashMap<String, String[]> servletRequestParameters = new HashMap<>();

	@Test
	public void testProcessSpan_withValidLongValueReturnsThatValue() throws Exception {
		// Given
		addServletParameter("123");
		ClientSpanLongTagProcessor clientSpanLongTagProcessor = new ClientSpanLongTagProcessor(TYPE_ALL, TAG, REQUEST_PARAMETER_NAME);

		// When
		SpanWrapper span = runProcessor(clientSpanLongTagProcessor);

		// Then
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = mockTracer.finishedSpans().get(0);
		assertThat(mockSpan.tags().get(TAG)).isEqualTo(123L);
		assertThat(span.getNumberTag(Tags.SAMPLING_PRIORITY.getKey())).isNotEqualTo(0);
	}

	private void addServletParameter(String parameterValue) {
		servletRequestParameters.put(REQUEST_PARAMETER_NAME, new String[]{parameterValue});
	}

	@Test
	public void testProcessSpan_withInvalidValueReturnsNull() throws Exception {
		// Given
		addServletParameter("invalid");
		ClientSpanLongTagProcessor clientSpanLongTagProcessor = new ClientSpanLongTagProcessor(TYPE_ALL, TAG, REQUEST_PARAMETER_NAME);

		// When
		SpanWrapper span = runProcessor(clientSpanLongTagProcessor);

		// Then
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = mockTracer.finishedSpans().get(0);
		assertThat(mockSpan.tags()).doesNotContainKey(TAG);
		assertThat(span.getNumberTag(Tags.SAMPLING_PRIORITY.getKey())).isNotEqualTo(0);
	}

	@Test
	public void testProcessSpan_withUpperIgnoresIfGreater()  throws Exception {
		// Given
		addServletParameter("6");
		ClientSpanLongTagProcessor clientSpanLongTagProcessor = new ClientSpanLongTagProcessor(TYPE_ALL, TAG, REQUEST_PARAMETER_NAME)
				.upperBound(5);

		// When
		SpanWrapper span = runProcessor(clientSpanLongTagProcessor);

		// Then
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = mockTracer.finishedSpans().get(0);
		assertThat(mockSpan.tags()).doesNotContainKeys(TAG);
		assertThat(span.getNumberTag(Tags.SAMPLING_PRIORITY.getKey())).isNotEqualTo(0);
	}

	@Test
	public void testProcessSpan_withUpperBoundDiscardsIfGreater()  throws Exception {
		// Given
		addServletParameter("6");
		ClientSpanLongTagProcessor clientSpanLongTagProcessor = new ClientSpanLongTagProcessor(TYPE_ALL, TAG, REQUEST_PARAMETER_NAME)
				.upperBound(5)
				.discardSpanOnBoundViolation(true);

		// When
		SpanWrapper span = runProcessor(clientSpanLongTagProcessor);

		// Then
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = mockTracer.finishedSpans().get(0);
		assertThat(mockSpan.tags()).doesNotContainKeys(TAG);
		assertThat(span.getNumberTag(Tags.SAMPLING_PRIORITY.getKey())).isEqualTo(0);
	}

	@Test
	public void testProcessSpan_withUpperBoundAllowingIfEquals()  throws Exception {
		// Given
		addServletParameter("6");
		ClientSpanLongTagProcessor clientSpanLongTagProcessor = new ClientSpanLongTagProcessor(TYPE_ALL, TAG, REQUEST_PARAMETER_NAME)
				.upperBound(6)
				.discardSpanOnBoundViolation(true);

		// When
		SpanWrapper span = runProcessor(clientSpanLongTagProcessor);

		// Then
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = mockTracer.finishedSpans().get(0);
		assertThat(mockSpan.tags().get(TAG)).isEqualTo(6L);
		assertThat(span.getNumberTag(Tags.SAMPLING_PRIORITY.getKey())).isNotEqualTo(0);
	}

	@Test
	public void testProcessSpan_withLowerBoundIgnoresIfLower()  throws Exception {
		// Given
		addServletParameter("4");
		ClientSpanLongTagProcessor clientSpanLongTagProcessor = new ClientSpanLongTagProcessor(TYPE_ALL, TAG, REQUEST_PARAMETER_NAME)
				.lowerBound(5);

		// When
		SpanWrapper span = runProcessor(clientSpanLongTagProcessor);

		// Then
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = mockTracer.finishedSpans().get(0);
		assertThat(mockSpan.tags()).doesNotContainKeys(TAG);
		assertThat(span.getNumberTag(Tags.SAMPLING_PRIORITY.getKey())).isNotEqualTo(0);
	}

	@Test
	public void testProcessSpan_withLowerBoundDiscardsIfLower()  throws Exception {
		// Given
		addServletParameter("4");
		ClientSpanLongTagProcessor clientSpanLongTagProcessor = new ClientSpanLongTagProcessor(TYPE_ALL, TAG, REQUEST_PARAMETER_NAME)
				.lowerBound(5)
				.discardSpanOnBoundViolation(true);

		// When
		SpanWrapper span = runProcessor(clientSpanLongTagProcessor);

		// Then
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = mockTracer.finishedSpans().get(0);
		assertThat(mockSpan.tags()).doesNotContainKeys(TAG);
		assertThat(span.getNumberTag(Tags.SAMPLING_PRIORITY.getKey())).isEqualTo(0);
	}

	@Test
	public void testProcessSpan_withLowerBoundAllowingIfEquals()  throws Exception {
		// Given
		addServletParameter("5");
		ClientSpanLongTagProcessor clientSpanLongTagProcessor = new ClientSpanLongTagProcessor(TYPE_ALL, TAG, REQUEST_PARAMETER_NAME)
				.lowerBound(5)
				.discardSpanOnBoundViolation(true);

		// When
		SpanWrapper span = runProcessor(clientSpanLongTagProcessor);

		// Then
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = mockTracer.finishedSpans().get(0);
		assertThat(mockSpan.tags().get(TAG)).isEqualTo(5L);
		assertThat(span.getNumberTag(Tags.SAMPLING_PRIORITY.getKey())).isNotEqualTo(0);
	}

	private SpanWrapper runProcessor(ClientSpanLongTagProcessor clientSpanLongTagProcessor) {
		clientSpanLongTagProcessor.processSpanBuilderImpl(spanBuilder, servletRequestParameters);
		final SpanWrapper span = spanBuilder.start();
		clientSpanLongTagProcessor.processSpanImpl(span, servletRequestParameters);
		span.finish();
		return span;
	}

	@Before
	public void setUp() {
		this.mockTracer = new MockTracer();
		SpanWrappingTracer spanWrappingTracer = new SpanWrappingTracer(mockTracer, asList(new SpanContextSpanEventListener(), new SpanFinalizer()));
		this.spanBuilder = spanWrappingTracer.buildSpan("");
		this.servletRequestParameters = new HashMap<>();
	}
}
