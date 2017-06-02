package org.stagemonitor.web.servlet;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StagemonitorFileServletTest {

	private StagemonitorFileServlet fileServlet;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;

	@Before
	public void setUp() throws Exception {
		fileServlet = new StagemonitorFileServlet();
		fileServlet.init(new MockServletConfig(new MockServletContext()));

		request = new MockHttpServletRequest("GET", "/stagemonitor/static/test.js");
		response = new MockHttpServletResponse();
	}

	@Test
	public void testGetStaticResource() throws Exception {
		request.setRequestURI("/stagemonitor/static/test.html");

		fileServlet.service(request, response);

		assertEquals(200, response.getStatus());
		assertEquals("test", response.getContentAsString());
		assertTrue(response.getContentType().equals("text/html")
				|| response.getContentType().equals("application/octet-stream"));
	}

	@Test
	public void testGetStaticResourceDirUp() throws Exception {
		request.setRequestURI("/stagemonitor/static/../test2.js");

		fileServlet.service(request, response);

		assertEquals(404, response.getStatus());
		assertEquals("", response.getContentAsString());
	}
}
