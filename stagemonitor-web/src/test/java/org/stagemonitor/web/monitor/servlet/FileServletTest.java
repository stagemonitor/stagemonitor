package org.stagemonitor.web.monitor.servlet;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

public class FileServletTest {

	@Test
	public void testService() throws Exception {
		final FileServlet fileServlet = new FileServlet("/static/test.js");
		final MockServletContext mockServletContext = new MockServletContext();
		final MockHttpServletRequest request = new MockHttpServletRequest(mockServletContext, "GET", "/stagemonitor/static/test.js");
		final MockHttpServletResponse response = new MockHttpServletResponse();
		fileServlet.service(request, response);

		Assert.assertEquals("test", response.getContentAsString());
	}
}
