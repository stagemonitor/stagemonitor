package org.stagemonitor.web.servlet;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.DispatcherServlet;
import org.stagemonitor.tracing.profiler.CallStackElement;
import org.stagemonitor.tracing.profiler.Profiler;

import javax.servlet.Servlet;

import static org.junit.Assert.assertEquals;

public class ServletProfilingTransformerTest {

	@Test
	public void testProfileServlet() throws Exception {
		Servlet servlet = new DispatcherServlet();

		final CallStackElement total = Profiler.activateProfiling("total");
		servlet.service(new MockHttpServletRequest(), new MockHttpServletResponse());
		Profiler.stop();

		final CallStackElement serviceCall = total.getChildren().iterator().next();
		assertEquals("FrameworkServlet#service", serviceCall.getShortSignature());
	}

	@Test
	public void testDontProfileStagemonitorServlet() throws Exception {
		Servlet servlet = new StagemonitorFileServlet();

		final CallStackElement total = Profiler.activateProfiling("total");
		servlet.service(new MockHttpServletRequest(), new MockHttpServletResponse());
		Profiler.stop();

		assertEquals(0, total.getChildren().size());
	}
}
