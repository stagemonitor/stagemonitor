package org.stagemonitor;

import org.junit.Test;
import org.mockito.Mockito;
import org.stagemonitor.web.monitor.filter.AbstractExclusionFilter;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
		assertExcludes("/context-path/exclude1");
		assertExcludes("/context-path/exclude2/bla/blubb");
		assertExcludes("/context-path/exclude3/");
		assertExcludes("/context-path/exclude2bla");

		assertIncludes("/context-path/exclude3");
		assertIncludes("/context-path/included");
		assertIncludes("/context-path/included/exclude1");
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
		when(mockRequest.getContextPath()).thenReturn("/context-path");
		testFilter.doFilter(mockRequest, null, mock(FilterChain.class));
		verify(testFilter, times(notExclutedCount)).doFilterInternal((ServletRequest) any(), (ServletResponse) any(), (FilterChain) any());
	}
}
