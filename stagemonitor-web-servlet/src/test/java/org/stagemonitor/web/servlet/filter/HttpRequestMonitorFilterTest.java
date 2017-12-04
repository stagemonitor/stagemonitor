package org.stagemonitor.web.servlet.filter;

import com.codahale.metrics.health.HealthCheckRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.tracing.RequestMonitor;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.tracing.B3Propagator;
import org.stagemonitor.tracing.wrapper.SpanWrappingTracer;
import org.stagemonitor.web.servlet.ServletPlugin;

import java.io.IOException;
import java.util.Arrays;

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
import io.opentracing.util.ThreadLocalScopeManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class HttpRequestMonitorFilterTest {

	private ConfigurationRegistry configuration = mock(ConfigurationRegistry.class);
	private ServletPlugin servletPlugin = spy(new ServletPlugin());
	private CorePlugin corePlugin = mock(CorePlugin.class);
	private TracingPlugin tracingPlugin = mock(TracingPlugin.class);
	private HttpRequestMonitorFilter httpRequestMonitorFilter;
	private String testHtml = "<html><head></head><body></body></html>";

	@Before
	public void before() throws Exception {
		/*
		when(requestMonitor.monitor(any(MonitoredRequest.class))).then(new Answer<SpanContextInformation>() {
			@Override
			public SpanContextInformation answer(InvocationOnMock invocation) throws Throwable {
				MonitoredRequest request = (MonitoredRequest) invocation.getArguments()[0];
				request.execute();
				doReturn("testName").when(spanContext).getOperationName();
				return spanContext;
			}
		});
		*/

		doReturn(servletPlugin).when(configuration).getConfig(ServletPlugin.class);
		doReturn(tracingPlugin).when(configuration).getConfig(TracingPlugin.class);
		doReturn(corePlugin).when(configuration).getConfig(CorePlugin.class);
		doReturn(true).when(servletPlugin).isWidgetEnabled();
		doReturn(true).when(servletPlugin).isWidgetAndStagemonitorEndpointsAllowed(any(HttpServletRequest.class));
		doReturn(true).when(servletPlugin).isClientSpanCollectionEnabled();
		doReturn(true).when(servletPlugin).isClientSpanCollectionInjectionEnabled();
		doReturn(true).when(corePlugin).isStagemonitorActive();
		doReturn(1000000d).when(tracingPlugin).getProfilerRateLimitPerMinute();
		when(tracingPlugin.isSampled(any())).thenReturn(true);
		final RequestMonitor requestMonitor = new RequestMonitor(configuration, mock(Metric2Registry.class));
		doReturn(requestMonitor).when(tracingPlugin).getRequestMonitor();
		final SpanWrappingTracer spanWrappingTracer = new SpanWrappingTracer(new MockTracer(new ThreadLocalScopeManager(), new B3Propagator()),
				Arrays.asList(new SpanContextInformation.SpanContextSpanEventListener(), new SpanContextInformation.SpanFinalizer()));
		doAnswer(invocation -> {
			spanWrappingTracer.addEventListenerFactory(invocation.getArgument(0));
			return null;
		}).when(tracingPlugin).addSpanEventListenerFactory(any());
		doReturn(spanWrappingTracer).when(tracingPlugin).getTracer();
		doReturn("testApplication").when(corePlugin).getApplicationName();
		doReturn("testInstance").when(corePlugin).getInstanceName();
		servletPlugin.initializePlugin(new StagemonitorPlugin.InitArguments(mock(Metric2Registry.class), configuration,
				mock(MeasurementSession.class), mock(HealthCheckRegistry.class)));

		initFilter();
		assertThat(tracingPlugin.getTracer().scopeManager().active()).isNull();
	}

	@After
	public void tearDown() throws Exception {
		assertThat(tracingPlugin.getTracer().scopeManager().active()).isNull();
	}

	private void initFilter() throws Exception {
		final ServletContext servlet3Context = mock(ServletContext.class);
		doReturn(3).when(servlet3Context).getMajorVersion();
		doReturn("").when(servlet3Context).getContextPath();
		doReturn(mock(ServletRegistration.Dynamic.class)).when(servlet3Context).addServlet(anyString(), any(Servlet.class));
		final FilterConfig filterConfig = spy(new MockFilterConfig());
		doReturn(servlet3Context).when(filterConfig).getServletContext();

		httpRequestMonitorFilter = new HttpRequestMonitorFilter(configuration);
		httpRequestMonitorFilter.initInternal(filterConfig);
	}

	@Test
	public void testWidgetInjector() throws IOException, ServletException {
		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		httpRequestMonitorFilter.doFilter(requestWithAccept("text/html"), servletResponse, writeInResponseWhenCallingDoFilter(testHtml));

		final String response = servletResponse.getContentAsString();
		assertThat(response).startsWith("<html><head><script");
		assertThat(response).endsWith("</body></html>");
		assertThat(response).contains("window.StagemonitorLoaded");
		assertThat(response).contains("'/stagemonitor/public/eum.js'");
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
		doReturn(false).when(servletPlugin).isClientSpanCollectionEnabled();
		doReturn(false).when(servletPlugin).isWidgetAndStagemonitorEndpointsAllowed(any(HttpServletRequest.class));
		doReturn(false).when(servletPlugin).isWidgetEnabled();
		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		httpRequestMonitorFilter.doFilter(requestWithAccept("text/html"), servletResponse, writeInResponseWhenCallingDoFilter(testHtml));

		assertThat(servletResponse.getContentAsString()).isEqualTo(testHtml);
	}

	@Test
	public void testWidgetShouldNotBeInjectedIfHtmlIsNotAcceptable() throws IOException, ServletException {
		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		httpRequestMonitorFilter.doFilter(requestWithAccept("application/json"), servletResponse, writeInResponseWhenCallingDoFilter(testHtml));

		assertThat(servletResponse.getContentAsString()).isEqualTo(testHtml);
	}

	@Test
	public void testWidgetInjectorWithMultipleBodyTags() throws IOException, ServletException {
		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		final String html = "<html><body></body><body></body><body></body><body>asdf</body></html>";

		httpRequestMonitorFilter.doFilter(requestWithAccept("text/html"), servletResponse, writeInResponseWhenCallingDoFilter(html));

		assertThat(servletResponse.getContentAsString()).startsWith("<html><body></body><body></body><body></body><body>asdf");
		assertThat(servletResponse.getContentAsString()).endsWith("</body></html>");
		assertThat(servletResponse.getContentAsString()).contains("window.StagemonitorLoaded");
	}

	@Test
	public void testWidgetInjectorWithMultipleHeadTags() throws IOException, ServletException {
		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		final String html = "<html><head></head><body><script>var html = '<head></head>'</script></body></html>";

		httpRequestMonitorFilter.doFilter(requestWithAccept("text/html"), servletResponse, writeInResponseWhenCallingDoFilter(html));

		final String response = servletResponse.getContentAsString();
		assertThat(response).startsWith("<html><head><script");
		assertThat(response).endsWith("</body></html>");
		assertThat(response).contains("window.StagemonitorLoaded");
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
