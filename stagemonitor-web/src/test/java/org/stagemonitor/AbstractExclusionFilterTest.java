package org.stagemonitor;

import org.junit.Test;
import org.mockito.Mockito;
import org.stagemonitor.web.monitor.filter.AbstractExclusionFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.mockito.Mockito.*;

public class AbstractExclusionFilterTest {

	private AbstractExclusionFilter testFilter = Mockito.spy(new TestExclusionFilter());
	private FilterConfig filterConfigMock = mock(FilterConfig.class);
	private HttpServletRequest mockRequest = mock(HttpServletRequest.class);

	private static class TestExclusionFilter extends AbstractExclusionFilter {
		@Override
		public void doFilterInternal(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		}
	}

	@Test
	public void testExclude() throws Exception {
		when(filterConfigMock.getInitParameter("exclude")).thenReturn("exclude1,/exclude2,  /exclude3/");
		testFilter.init(filterConfigMock);
		assertExcludes("/exclude1");
		assertExcludes("/exclude2/bla/blubb");
		assertExcludes("/exclude3/");
		assertExcludes("/exclude2bla");

		assertIncludes("/exclude3");
		assertIncludes("/included");
		assertIncludes("/included/exclude1");
	}

	@Test
	public void testNotExclude() throws Exception {
		testFilter.init(filterConfigMock);
		assertIncludes("/exclude3");
	}

	private void assertIncludes(String url) throws Exception {
		assertExcludes(url, false);
	}

	private void assertExcludes(String url) throws Exception {
		assertExcludes(url, true);
	}

	private int notExclutedCount = 0;

	private void assertExcludes(String url, boolean excluded) throws Exception {
		if (!excluded)
			notExclutedCount++;

		when(mockRequest.getRequestURI()).thenReturn(url);
		testFilter.doFilter(mockRequest, null, mock(FilterChain.class));
		verify(testFilter, times(notExclutedCount)).doFilterInternal((ServletRequest) any(), (ServletResponse) any(), (FilterChain) any());
	}
}
