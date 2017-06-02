package org.stagemonitor.web.servlet.filter;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CompositeFilter;
import org.stagemonitor.tracing.profiler.CallStackElement;
import org.stagemonitor.tracing.profiler.Profiler;

import javax.servlet.Filter;

import static org.junit.Assert.assertEquals;

@Ignore
public class FilterProfilingTransformerTest {

	@Test
	public void testProfileServlet() throws Exception {
		Filter filter = new CompositeFilter();

		final CallStackElement total = Profiler.activateProfiling("total");
		filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());
		Profiler.stop();

		final CallStackElement serviceCall = total.getChildren().iterator().next();
		assertEquals("CompositeFilter#doFilter", serviceCall.getShortSignature());
	}

	@Test
	public void testDontProfileStagemonitorServlet() throws Exception {
		Filter filter = new HttpRequestMonitorFilter();

		final CallStackElement total = Profiler.activateProfiling("total");
		filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());
		Profiler.stop();

		assertEquals(0, total.getChildren().size());
	}

}
