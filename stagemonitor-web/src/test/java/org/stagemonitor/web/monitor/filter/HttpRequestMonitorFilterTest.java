package org.stagemonitor.web.monitor.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.codahale.metrics.MetricRegistry;
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
import org.stagemonitor.requestmonitor.MonitoredRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.HttpRequestTrace;
import org.stagemonitor.web.monitor.rum.BoomerangJsHtmlInjector;

public class HttpRequestMonitorFilterTest {

	private Configuration configuration = mock(Configuration.class);
	private WebPlugin webPlugin = mock(WebPlugin.class);
	private CorePlugin corePlugin = mock(CorePlugin.class);
	private RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
	private RequestMonitor.RequestInformation requestInformation = mock(RequestMonitor.RequestInformation.class);
	private HttpRequestTrace requestTrace = mock(HttpRequestTrace.class);
	private HttpRequestMonitorFilter httpRequestMonitorFilter;
	private String testHtml = "<html><body></body></html>";

	@Before
	public void before() throws Exception {
		final RequestMonitor requestMonitor = mock(RequestMonitor.class);
		when(requestMonitor.monitor(any(MonitoredRequest.class))).then(new Answer<RequestMonitor.RequestInformation<?>>() {
			@Override
			public RequestMonitor.RequestInformation<?> answer(InvocationOnMock invocation) throws Throwable {
				MonitoredRequest<?> request = (MonitoredRequest<?>) invocation.getArguments()[0];
				request.execute();
				when(requestTrace.toJson()).thenReturn("");
				when(requestTrace.getName()).thenReturn("testName");
				when(requestInformation.getRequestTrace()).thenReturn(requestTrace);
				return requestInformation;
			}
		});

		when(configuration.getConfig(WebPlugin.class)).thenReturn(webPlugin);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(webPlugin.isWidgetEnabled()).thenReturn(true);
		when(webPlugin.isWidgetAndStagemonitorEndpointsAllowed(any(HttpServletRequest.class), any(Configuration.class))).thenReturn(true);
		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		when(requestMonitorPlugin.isCollectRequestStats()).thenReturn(true);
		when(requestMonitorPlugin.getCallStackEveryXRequestsToGroup()).thenReturn(1);
		when(requestMonitorPlugin.getRequestMonitor()).thenReturn(requestMonitor);
		when(corePlugin.getApplicationName()).thenReturn("testApplication");
		when(corePlugin.getInstanceName()).thenReturn("testInstance");

		initFilter();
	}

	private void initFilter() throws Exception {
		final ServletContext servlet3Context = mock(ServletContext.class);
		when(servlet3Context.getMajorVersion()).thenReturn(3);
		when(servlet3Context.getContextPath()).thenReturn("");
		when(servlet3Context.addServlet(anyString(), any(Servlet.class))).thenReturn(mock(ServletRegistration.Dynamic.class));
		final FilterConfig filterConfig = spy(new MockFilterConfig());
		when(filterConfig.getServletContext()).thenReturn(servlet3Context);

		httpRequestMonitorFilter = new HttpRequestMonitorFilter(configuration, mock(MetricRegistry.class));
		httpRequestMonitorFilter.initInternal(filterConfig);
	}

	@Test
	public void testWidgetInjector() throws IOException, ServletException {
		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		httpRequestMonitorFilter.doFilter(requestWithAccept("text/html"), servletResponse, writeInResponseWhenCallingDoFilter(testHtml));

		assertTrue(servletResponse.getContentAsString().startsWith("<html><body>"));
		assertTrue(servletResponse.getContentAsString().endsWith("</body></html>"));
		assertFalse(servletResponse.getContentAsString().contains("beacon_url"));
		assertTrue(servletResponse.getContentAsString().contains("window.StagemonitorLoaded"));
	}

	@Test
	public void testBinaryData() throws IOException, ServletException {
		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		httpRequestMonitorFilter.doFilter(requestWithAccept("text/html"), servletResponse,
				writeBinaryDataInResponseWhenCallingDoFilter(new byte[] {1}));

		assertEquals(1, servletResponse.getContentAsByteArray().length);
		assertEquals(1, servletResponse.getContentAsByteArray()[0]);
	}

	private MockHttpServletRequest requestWithAccept(String accept) {
		final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		mockHttpServletRequest.addHeader("accept", accept);
		return mockHttpServletRequest;
	}

	@Test
	public void testWidgetShouldNotBeInjectedIfInjectionDisabled() throws IOException, ServletException {
		when(webPlugin.isRealUserMonitoringEnabled()).thenReturn(false);
		when(webPlugin.isWidgetAndStagemonitorEndpointsAllowed(any(HttpServletRequest.class), any(Configuration.class))).thenReturn(false);
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

		assertTrue(servletResponse.getContentAsString().startsWith("<html><body></body><body></body><body></body><body>asdf"));
		assertTrue(servletResponse.getContentAsString().endsWith("</body></html>"));
		assertFalse(servletResponse.getContentAsString().contains("beacon_url"));
		assertTrue(servletResponse.getContentAsString().contains("window.StagemonitorLoaded"));
	}

	private FilterChain writeInResponseWhenCallingDoFilter(final String html) throws IOException, ServletException {
		final FilterChain filterChain = mock(FilterChain.class);
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
				if (Math.random() > 0.5) {
					System.out.println("using writer");
					response.getWriter().write(html);
				} else {
					System.out.println("using output stream");
					response.getOutputStream().print(html);
				}
				response.flushBuffer();
				response.setContentType("text/html");
				return null;
			}
		}).when(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
		return filterChain;
	}

	private FilterChain writeBinaryDataInResponseWhenCallingDoFilter(final byte[] bytes) throws IOException, ServletException {
		final FilterChain filterChain = mock(FilterChain.class);
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
				response.getOutputStream().write(bytes);
				response.flushBuffer();
				response.setContentType("text/html");
				return null;
			}
		}).when(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
		return filterChain;
	}

	@Test
	public void testRUM() throws Exception {
		when(webPlugin.isRealUserMonitoringEnabled()).thenReturn(true);
		when(webPlugin.isWidgetEnabled()).thenReturn(false);
		when(webPlugin.isWidgetAndStagemonitorEndpointsAllowed(any(HttpServletRequest.class), any(Configuration.class))).thenReturn(false);
		initFilter();

		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		httpRequestMonitorFilter.doFilter(requestWithAccept("text/html"), servletResponse, writeInResponseWhenCallingDoFilter(testHtml));

		Assert.assertEquals("<html><body><script src=\"/stagemonitor/public/static/rum/" + BoomerangJsHtmlInjector.BOOMERANG_FILENAME + "\"></script>\n" +
				"<script>\n" +
				"   BOOMR.init({\n" +
				"      beacon_url: '/stagemonitor/public/rum',\n" +
				"      log: null\n" +
				"   });\n" +
				"   BOOMR.addVar(\"requestId\", \"null\");\n" +
				"   BOOMR.addVar(\"requestName\", \"testName\");\n" +
				"   BOOMR.addVar(\"serverTime\", 0);\n" +
				"</script></body></html>", servletResponse.getContentAsString());
	}

}