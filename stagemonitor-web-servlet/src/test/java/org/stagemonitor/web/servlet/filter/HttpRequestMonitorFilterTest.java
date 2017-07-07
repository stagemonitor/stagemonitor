package org.stagemonitor.web.servlet.filter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.tracing.RequestMonitor;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.tracing.B3Propagator;
import org.stagemonitor.tracing.wrapper.SpanWrappingTracer;
import org.stagemonitor.web.servlet.ServletPlugin;

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

import io.opentracing.mock.MockTracer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class HttpRequestMonitorFilterTest {

	private ConfigurationRegistry configuration = mock(ConfigurationRegistry.class);
	private ServletPlugin servletPlugin = mock(ServletPlugin.class);
	private CorePlugin corePlugin = mock(CorePlugin.class);
	private TracingPlugin tracingPlugin = mock(TracingPlugin.class);
	private HttpRequestMonitorFilter httpRequestMonitorFilter;
	private String testHtml = "<html><body></body></html>";

	@Before
	public void before() throws Exception {
		/*
		when(requestMonitor.monitor(any(MonitoredRequest.class))).then(new Answer<SpanContextInformation>() {
			@Override
			public SpanContextInformation answer(InvocationOnMock invocation) throws Throwable {
				MonitoredRequest request = (MonitoredRequest) invocation.getArguments()[0];
				request.execute();
				when(spanContext.getOperationName()).thenReturn("testName");
				return spanContext;
			}
		});
		*/

		when(configuration.getConfig(ServletPlugin.class)).thenReturn(servletPlugin);
		when(configuration.getConfig(TracingPlugin.class)).thenReturn(tracingPlugin);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(servletPlugin.isWidgetEnabled()).thenReturn(true);
		when(servletPlugin.isWidgetAndStagemonitorEndpointsAllowed(any(HttpServletRequest.class), any(ConfigurationRegistry.class))).thenReturn(true);
		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		when(tracingPlugin.getProfilerRateLimitPerMinute()).thenReturn(1000000d);
		final RequestMonitor requestMonitor = new RequestMonitor(configuration, mock(Metric2Registry.class));
		when(tracingPlugin.getRequestMonitor()).thenReturn(requestMonitor);
		final SpanWrappingTracer spanWrappingTracer = new SpanWrappingTracer(new MockTracer(new B3Propagator()));
		doAnswer(invocation -> {
			spanWrappingTracer.addEventListenerFactory(invocation.getArgument(0));
			return null;
		}).when(tracingPlugin).addSpanEventListenerFactory(any());
		when(tracingPlugin.getTracer()).thenReturn(spanWrappingTracer);
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

		httpRequestMonitorFilter = new HttpRequestMonitorFilter(configuration);
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
				writeBinaryDataInResponseWhenCallingDoFilter(new byte[]{1}));

		assertEquals(1, servletResponse.getContentAsByteArray().length);
		assertEquals(1, servletResponse.getContentAsByteArray()[0]);
	}

	private MockHttpServletRequest requestWithAccept(String accept) {
		final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest("GET", "/test");
		mockHttpServletRequest.addHeader("accept", accept);
		return mockHttpServletRequest;
	}

	@Test
	public void testWidgetShouldNotBeInjectedIfInjectionDisabled() throws IOException, ServletException {
		when(servletPlugin.isClientSpanCollectionEnabled()).thenReturn(false);
		when(servletPlugin.isWidgetAndStagemonitorEndpointsAllowed(any(HttpServletRequest.class), any(ConfigurationRegistry.class))).thenReturn(false);
		when(servletPlugin.isWidgetEnabled()).thenReturn(false);
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

}
