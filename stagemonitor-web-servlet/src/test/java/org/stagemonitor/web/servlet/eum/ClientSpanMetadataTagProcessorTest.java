package org.stagemonitor.web.servlet.eum;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.web.servlet.ServletPlugin;
import org.stagemonitor.web.servlet.eum.ClientSpanMetadataTagProcessor.ClientSpanMetadataDefinition;

import java.util.HashMap;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.stagemonitor.web.servlet.eum.ClientSpanTagProcessor.MAX_LENGTH;

public class ClientSpanMetadataTagProcessorTest {

	private MockTracer tracer;
	private MockTracer.SpanBuilder spanBuilder;
	private HashMap<String, ClientSpanMetadataDefinition> whitelistedValues;
	private HashMap<String, String[]> servletParameters;
	private ClientSpanMetadataTagProcessor clientSpanMetadataTagProcessor;

	@Before
	public void setUp() {
		tracer = new MockTracer();
		spanBuilder = tracer.buildSpan("operationName");

		ServletPlugin servletPlugin = mock(ServletPlugin.class);
		whitelistedValues = new HashMap<>();
		when(servletPlugin.getWhitelistedClientSpanTags()).thenReturn(whitelistedValues);

		servletParameters = new HashMap<>();

		clientSpanMetadataTagProcessor = new ClientSpanMetadataTagProcessor(servletPlugin);
	}

	@Test
	public void processSpanBuilderImpl_testSpecificLengthString() throws Exception {
		addMetadataDefinition("lengthLimitedTag", "string(10)");
		addServletParameter("m_lengthLimitedTag", "0123456789ZZZZ");
		processSpanBuilderImpl(spanBuilder, servletParameters);

		assertThat(tracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = tracer.finishedSpans().get(0);
		assertThat(mockSpan.tags()).hasSize(1);
		assertThat(mockSpan.tags().get("lengthLimitedTag")).isEqualTo("0123456789");
	}

	@Test
	public void processSpanBuilderImpl_testSpecificLengthStringWithSpacesInDefinition() throws Exception {
		addMetadataDefinition("lengthLimitedTagWithSpaces", "  string  (  10  )  ");
		addServletParameter("m_lengthLimitedTagWithSpaces", "0123456789ZZZZ");
		processSpanBuilderImpl(spanBuilder, servletParameters);

		assertThat(tracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = tracer.finishedSpans().get(0);
		assertThat(mockSpan.tags()).hasSize(1);
		assertThat(mockSpan.tags().get("lengthLimitedTagWithSpaces")).isEqualTo("0123456789");
	}

	@Test
	public void processSpanBuilderImpl_testNoExplicitLengthLimit() throws Exception {
		String value = "";
		for (int i = 0; i < 255; i++) {
			value += "0";
		}
		addServletParameter("m_tagWithoutExplicitLengthLimit", value);
		addMetadataDefinition("tagWithoutExplicitLengthLimit", "string");
		processSpanBuilderImpl(spanBuilder, servletParameters);

		assertThat(tracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = tracer.finishedSpans().get(0);
		assertThat(mockSpan.tags()).hasSize(1);
		assertThat(mockSpan.tags().get("tagWithoutExplicitLengthLimit").toString()).hasSize(MAX_LENGTH);
	}

	@Test
	public void processSpanBuilderImpl_testMultipleParameters() throws Exception {
		addServletParameter("m_string", "string");
		addServletParameter("m_boolean", "true");
		addServletParameter("m_number", "42.5");
		addMetadataDefinition("string", "string");
		addMetadataDefinition("boolean", "boolean");
		addMetadataDefinition("number", "number");
		processSpanBuilderImpl(spanBuilder, servletParameters);

		assertThat(tracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = tracer.finishedSpans().get(0);
		assertThat(mockSpan.tags()).hasSize(3);
		assertThat(mockSpan.tags().get("string").toString()).isEqualTo("string");
		assertThat(mockSpan.tags().get("boolean").toString()).isEqualTo("true");
		assertThat(mockSpan.tags().get("number").toString()).isEqualTo("42.5");
	}

	@Test
	public void processSpanBuilderImpl_testDiscardsUndefinedMetadata() throws Exception {
		addServletParameter("m_tagWithoutExplicitLengthLimit", "someValue");
		processSpanBuilderImpl(spanBuilder, servletParameters);

		assertThat(tracer.finishedSpans()).hasSize(1);
		MockSpan mockSpan = tracer.finishedSpans().get(0);
		assertThat(mockSpan.tags()).isEmpty();
	}

	@Test(expected = IllegalArgumentException.class)
	public void processSpanBuilderImpl_testThrowsIllegalArgumentExceptionOnInvalidDefinition() {
		addServletParameter("m_metadataname", "something");
		addMetadataDefinition("metadataname", "invalid");
		processSpanBuilderImpl(spanBuilder, servletParameters);
	}

	private void processSpanBuilderImpl(MockTracer.SpanBuilder spanBuilder, HashMap<String, String[]> servletParameters) {
		clientSpanMetadataTagProcessor.processSpanBuilderImpl(spanBuilder, servletParameters);
		spanBuilder.start().close();
	}

	private void addMetadataDefinition(String name, String definition) {
		whitelistedValues.put(name, new ClientSpanMetadataDefinition(definition));
	}

	private void addServletParameter(String name, String value) {
		servletParameters.put(name, new String[] {value});
	}

}
