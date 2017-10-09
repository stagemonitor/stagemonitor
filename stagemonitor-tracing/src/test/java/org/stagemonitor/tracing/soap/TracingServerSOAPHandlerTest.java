package org.stagemonitor.tracing.soap;

import com.uber.jaeger.context.TracingUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanWrappingTracer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TracingServerSOAPHandlerTest {

	private TracingServerSOAPHandler tracingServerSOAPHandler;
	private SoapTracingPlugin soapTracingPlugin;
	private MockTracer mockTracer;
	private SOAPMessageContext soapRequest;
	private SOAPMessageContext soapResponse;

	@Before
	public void setUp() throws Exception {
		final TracingPlugin tracingPlugin = mock(TracingPlugin.class);
		mockTracer = new MockTracer();
		when(tracingPlugin.getTracer()).thenReturn(new SpanWrappingTracer(mockTracer,
				Arrays.asList(new SpanContextInformation.SpanContextSpanEventListener(), new SpanContextInformation.SpanFinalizer())));
		soapTracingPlugin = mock(SoapTracingPlugin.class);
		tracingServerSOAPHandler = new TracingServerSOAPHandler(tracingPlugin, soapTracingPlugin);
		soapRequest = getSoapMessageContext("<request/>", "operationName");
		soapResponse = getSoapMessageContext("<response/>", "operationName");
		assertThat(TracingUtils.getTraceContext().isEmpty()).isTrue();
	}

	@After
	public void tearDown() throws Exception {
		assertThat(TracingUtils.getTraceContext().isEmpty()).isTrue();
	}

	@Test
	public void testServerRequest() throws Exception {
		tracingServerSOAPHandler.handleInboundSOAPMessage(soapRequest);
		tracingServerSOAPHandler.handleOutboundSOAPMessage(soapResponse);
		tracingServerSOAPHandler.close(soapResponse);
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		final MockSpan span = mockTracer.finishedSpans().get(0);
		assertThat(span.operationName()).isEqualTo("operationName");
		assertThat(span.tags()).containsAllEntriesOf(new HashMap<String, Object>() {{
			put(SpanUtils.OPERATION_TYPE, "soap");
			put(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
		}});
		assertThat(span.tags()).doesNotContainKeys("soap.request", "soap.response");
	}

	@Test
	public void testServerRequestWithRequestSoapMessage() throws Exception {
		when(soapTracingPlugin.isSoapServerRecordRequestMessages()).thenReturn(true);
		tracingServerSOAPHandler.handleInboundSOAPMessage(soapRequest);
		tracingServerSOAPHandler.handleOutboundSOAPMessage(soapResponse);
		tracingServerSOAPHandler.close(soapResponse);
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		final MockSpan span = mockTracer.finishedSpans().get(0);
		assertThat(span.tags()).doesNotContainKeys("soap.response");
		assertThat(span.tags()).containsEntry("soap.request", "<request/>");
	}

	@Test
	public void testServerRequestWithResponseSoapMessage() throws Exception {
		when(soapTracingPlugin.isSoapServerRecordResponseMessages()).thenReturn(true);
		tracingServerSOAPHandler.handleInboundSOAPMessage(soapRequest);
		tracingServerSOAPHandler.handleOutboundSOAPMessage(soapResponse);
		tracingServerSOAPHandler.close(soapResponse);
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		final MockSpan span = mockTracer.finishedSpans().get(0);
		assertThat(span.tags()).doesNotContainKeys("soap.request");
		assertThat(span.tags()).containsEntry("soap.response", "<response/>");
	}

	static SOAPMessageContext getSoapMessageContext(String soapMessage, String wsdlOperation) throws SOAPException, IOException {
		SOAPMessageContext soapMessageContext = mock(SOAPMessageContext.class);
		when(soapMessageContext.get(MessageContext.WSDL_OPERATION)).thenReturn(new QName("", wsdlOperation));
		SOAPMessage soapMessage1 = mock(SOAPMessage.class);
		when(soapMessageContext.getMessage()).thenReturn(soapMessage1);
		doAnswer(invocation -> {
			invocation.<OutputStream>getArgument(0).write(soapMessage.getBytes(StandardCharsets.UTF_8));
			return null;
		}).when(soapMessage1).writeTo(any());
		return soapMessageContext;
	}
}
