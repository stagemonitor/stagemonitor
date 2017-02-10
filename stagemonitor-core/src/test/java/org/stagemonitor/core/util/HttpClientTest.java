package org.stagemonitor.core.util;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.*;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpClientTest {

	private HttpClient httpClient = new HttpClient();
	private Server server;

	@Before
	public void setUp() throws Exception {
		server = new Server(41234);
	}

	@After
	public void tearDown() throws Exception {
		server.stop();
	}

	@Test
	public void testNoNullPointerExceptionOnSend() throws IOException {
		try {
			httpClient.send("POST", "incorrect-url", null, null);
		} catch (NullPointerException t) {
			org.junit.Assert.fail("Shouldn't throw NPE");
		} catch (Exception ignore) {
			//whatever
		}
	}

	@Test
	public void testBasicAuth() throws Exception {
		final boolean[] handled = {false};
		server.setHandler(new AbstractHandler() {
			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				baseRequest.setHandled(true);
				assertEquals("Basic dXNlcjpwYXNz", request.getHeader("Authorization"));
				handled[0] = true;
			}
		});
		server.start();

		assertEquals(200, httpClient.send("GET", "http://user:pass@localhost:41234/"));
		assertTrue(handled[0]);
	}
}
