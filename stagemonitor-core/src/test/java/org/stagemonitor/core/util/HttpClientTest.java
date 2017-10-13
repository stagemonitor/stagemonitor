package org.stagemonitor.core.util;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;
import org.stagemonitor.core.util.http.HttpRequestBuilder;
import org.stagemonitor.core.util.http.StatusCodeResponseHandler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpClientTest extends AbstractEmbeddedServerTest {

	private HttpClient httpClient = new HttpClient();

	@Test
	public void testBasicAuth() throws Exception {
		final boolean[] handled = {false};
		startWithHandler(new AbstractHandler() {
			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				baseRequest.setHandled(true);
				assertEquals("Basic dXNlcjpwYXNz", request.getHeader("Authorization"));
				handled[0] = true;
			}
		});

		assertEquals(Integer.valueOf(200), httpClient.send(HttpRequestBuilder.<Integer>forUrl("http://user:pass@localhost:" + getPort() + "/")
				.successHandler(new StatusCodeResponseHandler()).build()));
		assertTrue(handled[0]);
	}
}
