package de.isys.jawap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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

	@Before
	public void setUp() throws ServletException {
		when(filterConfigMock.getInitParameter("exclude")).thenReturn("exclude1,/exclude2,  /exclude3/");
		testFilter.init(filterConfigMock);
	}

	@Test
	public void testExclude() throws Exception {
		testHelper("/exclude1", 0);
		testHelper("/exclude2/bla/blubb", 0);
		testHelper("/exclude3/", 0);
		testHelper("/exclude2bla", 0);
		testHelper("/exclude3", 1);
		testHelper("/included", 2);
		testHelper("/included/exclude1", 3);
	}

	private void testHelper(String url, int wantedNumberOfInvocations) throws Exception {
		when(mockRequest.getRequestURI()).thenReturn(url);
		testFilter.doFilter(mockRequest, null, mock(FilterChain.class));
		verify(testFilter, times(wantedNumberOfInvocations)).doFilterInternal((ServletRequest) any(), (ServletResponse) any(), (FilterChain) any());
	}
}
