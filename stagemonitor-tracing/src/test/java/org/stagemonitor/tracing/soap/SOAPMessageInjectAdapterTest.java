package org.stagemonitor.tracing.soap;

import org.junit.Before;
import org.junit.Test;

import javax.xml.ws.handler.soap.SOAPMessageContext;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SOAPMessageInjectAdapterTest {

	private SOAPMessageInjectAdapter soapMessageInjectAdapter;
	private SOAPMessageContext soapMessageContext;

	@Before
	public void setUp() throws Exception {
		soapMessageContext = mock(SOAPMessageContext.class);
		soapMessageInjectAdapter = new SOAPMessageInjectAdapter(soapMessageContext);
	}

	@Test
	public void iterator() throws Exception {
		assertThatThrownBy(() -> soapMessageInjectAdapter.iterator()).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	public void put() throws Exception {
		soapMessageInjectAdapter.put("foo", "bar");
		verify(soapMessageContext).put(eq(SOAPMessageContext.HTTP_REQUEST_HEADERS), eq(singletonMap("foo", singletonList("bar"))));
	}

}
