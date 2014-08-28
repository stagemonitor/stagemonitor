package org.stagemonitor.web.monitor.filter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.Configuration;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class HttpRequestMonitorFilterTest {

	private Configuration configuration = mock(Configuration.class);

	@Before
	public void before() throws Exception {
		when(configuration.isStagemonitorWidgetEnabled()).thenReturn(true);
		when(configuration.isStagemonitorActive()).thenReturn(true);
		when(configuration.isCollectRequestStats()).thenReturn(true);
		when(configuration.getCallStackEveryXRequestsToGroup()).thenReturn(1);
	}

	@Test
	public void testWidgetInjector() throws IOException, ServletException {
		final HttpRequestMonitorFilter httpRequestMonitorFilter = spy(new HttpRequestMonitorFilter(configuration));
		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		final String html = "<html><body></body></html>";

		httpRequestMonitorFilter.doFilter(requestWithAccept("text/html"), servletResponse, writeInResponseWhenCallingDoFilter(html));

		final String expected = "<html><body><!-- injection-placeholder --></body></html>";
		Assert.assertEquals(expected, servletResponse.getContentAsString());
	}

	private MockHttpServletRequest requestWithAccept(String accept) {
		final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		mockHttpServletRequest.addHeader("accept", accept);
		return mockHttpServletRequest;
	}

	@Test
	public void testWidgetShouldNotBeInjectedIfInjectionDisabled() throws IOException, ServletException {
		when(configuration.isStagemonitorWidgetEnabled()).thenReturn(false);
		final HttpRequestMonitorFilter httpRequestMonitorFilter = spy(new HttpRequestMonitorFilter(configuration));
		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		final String html = "<html><body></body></html>";

		httpRequestMonitorFilter.doFilter(requestWithAccept("text/html"), servletResponse, writeInResponseWhenCallingDoFilter(html));

		final String expected = "<html><body></body></html>";
		Assert.assertEquals(expected, servletResponse.getContentAsString());
	}

	@Test
	public void testWidgetShouldNotBeInjectedIfHtmlIsNotAcceptable() throws IOException, ServletException {
		final HttpRequestMonitorFilter httpRequestMonitorFilter = spy(new HttpRequestMonitorFilter(configuration));
		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		final String html = "<html><body></body></html>";

		httpRequestMonitorFilter.doFilter(requestWithAccept("application/json"), servletResponse, writeInResponseWhenCallingDoFilter(html));

		final String expected = "<html><body></body></html>";
		Assert.assertEquals(expected, servletResponse.getContentAsString());
	}

	@Test
	public void testWidgetInjectorWithMultipleBodyTags() throws IOException, ServletException {
		final HttpRequestMonitorFilter httpRequestMonitorFilter = spy(new HttpRequestMonitorFilter(configuration));
		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		final String html = "<html><body></body><body></body><body></body><body>asdf</body></html>";

		httpRequestMonitorFilter.doFilter(requestWithAccept("text/html"), servletResponse, writeInResponseWhenCallingDoFilter(html));

		final String expected = "<html><body></body><body></body><body></body><body>asdf<!-- injection-placeholder --></body></html>";
		Assert.assertEquals(expected, servletResponse.getContentAsString());
	}

	private FilterChain writeInResponseWhenCallingDoFilter(final String html) throws IOException, ServletException {
		final FilterChain filterChain = mock(FilterChain.class);
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
				response.getWriter().write(html);
				response.flushBuffer();
				response.setContentType("text/html");
				return null;
			}
		}).when(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
		return filterChain;
	}
}