package org.stagemonitor.web.monitor.filter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.web.WebPlugin;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class HttpRequestMonitorFilterTest {

	private Configuration configuration = mock(Configuration.class);
	private WebPlugin webPlugin = mock(WebPlugin.class);
	private CorePlugin corePlugin = mock(CorePlugin.class);
	private RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
	private HttpRequestMonitorFilter httpRequestMonitorFilter;
	private String testHtml = "<html><body></body></html>";

	@Before
	public void before() throws Exception {
		when(configuration.getConfig(WebPlugin.class)).thenReturn(webPlugin);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(webPlugin.isWidgetEnabled()).thenReturn(true);
		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		when(requestMonitorPlugin.isCollectRequestStats()).thenReturn(true);
		when(requestMonitorPlugin.getCallStackEveryXRequestsToGroup()).thenReturn(1);
		when(corePlugin.getApplicationName()).thenReturn("testApplication");
		when(corePlugin.getInstanceName()).thenReturn("testInstance");
		final ServletContext servlet3Context = mock(ServletContext.class);
		when(servlet3Context.getMajorVersion()).thenReturn(3);
		when(servlet3Context.getContextPath()).thenReturn("");
		when(servlet3Context.addServlet(anyString(), any(Servlet.class))).thenReturn(mock(ServletRegistration.Dynamic.class));
		final FilterConfig filterConfig = spy(new MockFilterConfig());
		when(filterConfig.getServletContext()).thenReturn(servlet3Context);
		httpRequestMonitorFilter = spy(new HttpRequestMonitorFilter(configuration));
		httpRequestMonitorFilter.initInternal(filterConfig);
	}

	@Test
	public void testWidgetInjector() throws IOException, ServletException {
		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		httpRequestMonitorFilter.doFilter(requestWithAccept("text/html"), servletResponse, writeInResponseWhenCallingDoFilter(testHtml));

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
		when(webPlugin.isWidgetEnabled()).thenReturn(false);
		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		httpRequestMonitorFilter.doFilter(requestWithAccept("text/html"), servletResponse, writeInResponseWhenCallingDoFilter(testHtml));

		final String expected = "<html><body></body></html>";
		Assert.assertEquals(expected, servletResponse.getContentAsString());
	}

	@Test
	public void testWidgetShouldNotBeInjectedIfHtmlIsNotAcceptable() throws IOException, ServletException {
		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		httpRequestMonitorFilter.doFilter(requestWithAccept("application/json"), servletResponse, writeInResponseWhenCallingDoFilter(testHtml));

		final String expected = "<html><body></body></html>";
		Assert.assertEquals(expected, servletResponse.getContentAsString());
	}

	@Test
	public void testWidgetInjectorWithMultipleBodyTags() throws IOException, ServletException {
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