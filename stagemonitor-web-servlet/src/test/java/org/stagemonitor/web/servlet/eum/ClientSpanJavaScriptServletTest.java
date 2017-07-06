package org.stagemonitor.web.servlet.eum;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientSpanJavaScriptServletTest {

	@Test
	public void testDoGet_simple() throws Exception {
		// Given
		ClientSpanJavaScriptServlet clientSpanJavaScriptServlet = new ClientSpanJavaScriptServlet();
		final MockHttpServletRequest request = new MockHttpServletRequest();
		final MockHttpServletResponse response = new MockHttpServletResponse();

		// When
		clientSpanJavaScriptServlet.doGet(request, response);

		// Then
		assertThat(response.getContentAsString()).contains("EumObject");
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	public void testDoGet_conditionalGet() throws Exception {
		// Given
		ClientSpanJavaScriptServlet clientSpanJavaScriptServlet = new ClientSpanJavaScriptServlet();
		final MockHttpServletResponse response = new MockHttpServletResponse();

		// When
		clientSpanJavaScriptServlet.doGet(new MockHttpServletRequest(), response);
		final String etag = response.getHeader("etag");
		assertThat(etag).isNotNull();
		final MockHttpServletRequest conditionalGetRequest = new MockHttpServletRequest();
		conditionalGetRequest.addHeader("if-none-match", etag);
		final MockHttpServletResponse conditionalGetResponse = new MockHttpServletResponse();
		clientSpanJavaScriptServlet.doGet(conditionalGetRequest, conditionalGetResponse);

		// Then
		assertThat(conditionalGetResponse.getStatus()).isEqualTo(304);
		assertThat(conditionalGetResponse.getContentLength()).isZero();
	}

}
