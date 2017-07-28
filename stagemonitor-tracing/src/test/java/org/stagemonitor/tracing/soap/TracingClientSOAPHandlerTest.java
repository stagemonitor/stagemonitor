package org.stagemonitor.tracing.soap;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanWrappingTracer;

import java.util.Collections;
import java.util.HashMap;

import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.stagemonitor.tracing.soap.TracingServerSOAPHandlerTest.getSoapMessageContext;

public class TracingClientSOAPHandlerTest {

	private TracingClientSOAPHandler tracingClientSOAPHandler;
	private SoapTracingPlugin soapTracingPlugin;
	private MockTracer mockTracer;
	private SOAPMessageContext soapRequest;
	private SOAPMessageContext soapResponse;

	@Before
	public void setUp() throws Exception {
		final TracingPlugin tracingPlugin = mock(TracingPlugin.class);
		mockTracer = new MockTracer();
		when(tracingPlugin.getTracer()).thenReturn(new SpanWrappingTracer(mockTracer, Collections.singletonList(new SpanContextInformation.SpanContextSpanEventListener())));
		soapTracingPlugin = mock(SoapTracingPlugin.class);
		tracingClientSOAPHandler = new TracingClientSOAPHandler(tracingPlugin, soapTracingPlugin);
		soapRequest = getSoapMessageContext("<request/>", "operationName");
		soapResponse = getSoapMessageContext("<response/>", "operationName");
	}

	@Test
	public void testClientRequest() throws Exception {
		tracingClientSOAPHandler.handleOutboundSOAPMessage(soapRequest);
		tracingClientSOAPHandler.handleInboundSOAPMessage(soapResponse);
		tracingClientSOAPHandler.close(soapResponse);

		verify(soapRequest).put(eq(MessageContext.HTTP_REQUEST_HEADERS), any());

		assertThat(mockTracer.finishedSpans()).hasSize(1);
		final MockSpan span = mockTracer.finishedSpans().get(0);
		assertThat(span.operationName()).isEqualTo("operationName");
		assertThat(span.tags()).containsAllEntriesOf(new HashMap<String, Object>() {{
			put(SpanUtils.OPERATION_TYPE, "soap");
			put(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
		}});
		assertThat(span.tags()).doesNotContainKeys("soap.request", "soap.response");
	}

	@Test
	public void testServerRequestWithRequestSoapMessage() throws Exception {
		when(soapTracingPlugin.isSoapClientRecordRequestMessages()).thenReturn(true);
		tracingClientSOAPHandler.handleOutboundSOAPMessage(soapRequest);
		tracingClientSOAPHandler.handleInboundSOAPMessage(soapResponse);
		tracingClientSOAPHandler.close(soapResponse);
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		final MockSpan span = mockTracer.finishedSpans().get(0);
		assertThat(span.tags()).doesNotContainKeys("soap.response");
		assertThat(span.tags()).containsEntry("soap.request", "<request/>");
	}

	@Test
	public void testServerRequestWithResponseSoapMessage() throws Exception {
		when(soapTracingPlugin.isSoapClientRecordResponseMessages()).thenReturn(true);
		tracingClientSOAPHandler.handleOutboundSOAPMessage(soapRequest);
		tracingClientSOAPHandler.handleInboundSOAPMessage(soapResponse);
		tracingClientSOAPHandler.close(soapResponse);
		assertThat(mockTracer.finishedSpans()).hasSize(1);
		final MockSpan span = mockTracer.finishedSpans().get(0);
		assertThat(span.tags()).doesNotContainKeys("soap.request");
		assertThat(span.tags()).containsEntry("soap.response", "<response/>");
	}

}
