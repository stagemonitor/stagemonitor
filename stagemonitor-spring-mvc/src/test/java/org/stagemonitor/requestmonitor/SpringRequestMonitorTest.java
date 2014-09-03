package org.stagemonitor.requestmonitor;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.springmvc.SpringMvcPlugin;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.HttpRequestTrace;
import org.stagemonitor.web.monitor.SpringMVCRequestNameDeterminerAspect;
import org.stagemonitor.web.monitor.SpringMonitoredHttpRequest;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static com.codahale.metrics.MetricRegistry.name;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.runners.Parameterized.Parameters;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.StageMonitor.getMetricRegistry;
import static org.stagemonitor.core.util.GraphiteSanitizer.sanitizeGraphiteMetricSegment;

@RunWith(value = Parameterized.class)
public class SpringRequestMonitorTest {

	private MockHttpServletRequest mvcRequest = new MockHttpServletRequest("GET", "/test/requestName");
	private MockHttpServletRequest nonMvcRequest = new MockHttpServletRequest("GET", "/META-INF/resources/stagemonitor/static/jquery.js");
	private List<HandlerMapping> handlerMappings;
	private Configuration configuration = mock(Configuration.class);
	private RequestMonitor requestMonitor = new RequestMonitor(configuration);
	private SpringMVCRequestNameDeterminerAspect springMVCRequestNameDeterminerAspect = Mockito.spy(new SpringMVCRequestNameDeterminerAspect(configuration));
	private final boolean useNameDeterminerAspect;

	public SpringRequestMonitorTest(boolean useNameDeterminerAspect) {
		this.useNameDeterminerAspect = useNameDeterminerAspect;
	}

	@Parameters
	public static Collection<Object[]> data() {
		Object[][] data = new Object[][] { { true }, { false } };
		return Arrays.asList(data);
	}

	@Before
	public void before() throws Exception {
		// the purpose of this class is to obtain a instance to a Method, because Method objects can't be mocked as they are final
		class Test { public void testGetRequestName() {} }
		handlerMappings = Arrays.asList(
				createExceptionThrowingHandlerMapping(),
				createHandlerMappingNotReturningHandlerMethod(),
				createHandlerMapping(mvcRequest, Test.class.getMethod("testGetRequestName"))
		);
		getMetricRegistry().removeMatching(new MetricFilter() {
			@Override
			public boolean matches(String name, Metric metric) {
				return true;
			}
		});
		when(configuration.isStagemonitorActive()).thenReturn(true);
		when(configuration.getBoolean(RequestMonitorPlugin.COLLECT_REQUEST_STATS)).thenReturn(true);
		when(configuration.getPatternMap(WebPlugin.STAGEMONITOR_GROUP_URLS
		)).thenReturn(Collections.singletonMap(Pattern.compile("(.*).js$"), "*.js"));
	}

	@After
	public void after() {
		verify(springMVCRequestNameDeterminerAspect, times(useNameDeterminerAspect ? 1 : 0))
				.aroundGetHandler(any(HttpServletRequest.class), any(HandlerExecutionChain.class));
	}

	private HandlerMapping createExceptionThrowingHandlerMapping() throws Exception {
		HandlerMapping requestMappingHandlerMapping = mock(HandlerMapping.class);
		when(requestMappingHandlerMapping.getHandler((HttpServletRequest) any())).thenThrow(new Exception());
		return requestMappingHandlerMapping;
	}

	private HandlerMapping createHandlerMappingNotReturningHandlerMethod() throws Exception {
		HandlerMapping requestMappingHandlerMapping = mock(HandlerMapping.class);
		HandlerExecutionChain handlerExecutionChain = mock(HandlerExecutionChain.class);

		when(handlerExecutionChain.getHandler()).thenReturn(new Object());
		when(requestMappingHandlerMapping.getHandler((HttpServletRequest) any())).thenReturn(handlerExecutionChain);
		return requestMappingHandlerMapping;
	}

	private HandlerMapping createHandlerMapping(MockHttpServletRequest request, Method requestMappingMethod) throws Exception {
		HandlerMapping requestMappingHandlerMapping = mock(HandlerMapping.class);
		HandlerExecutionChain handlerExecutionChain = mock(HandlerExecutionChain.class);
		HandlerMethod handlerMethod = mock(HandlerMethod.class);

		when(handlerMethod.getMethod()).thenReturn(requestMappingMethod);
		when(handlerExecutionChain.getHandler()).thenReturn(handlerMethod);
		when(requestMappingHandlerMapping.getHandler(request)).thenReturn(handlerExecutionChain);
		return requestMappingHandlerMapping;
	}

	@Test
	public void testRequestMonitorMvcRequest() throws Exception {
		when(configuration.getBoolean(SpringMvcPlugin.MONITOR_ONLY_SPRING_MVC_REQUESTS)).thenReturn(false);

		SpringMonitoredHttpRequest monitoredRequest = createSpringMonitoredHttpRequest(mvcRequest);
		registerAspect(monitoredRequest, mvcRequest, handlerMappings.get(2).getHandler(mvcRequest));
		final RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation = requestMonitor.monitor(monitoredRequest);

		assertEquals(1, requestInformation.getRequestTimer().getCount());
		assertEquals("Test-Get-Request-Name", requestInformation.getTimerName());
		assertEquals("Test Get Request Name", requestInformation.getRequestTrace().getName());
		assertEquals("/test/requestName", requestInformation.getRequestTrace().getUrl());
		assertEquals(Integer.valueOf(200), requestInformation.getRequestTrace().getStatusCode());
		assertEquals("GET", requestInformation.getRequestTrace().getMethod());
		Assert.assertNull(requestInformation.getExecutionResult());
		assertNotNull(getMetricRegistry().getTimers().get(name("request", "Test-Get-Request-Name", "server", "time", "total")));
		verify(monitoredRequest, times(1)).onPostExecute(anyRequestInformation());
		verify(monitoredRequest, times(useNameDeterminerAspect ? 0 : 1)).getRequestName();
	}

	@Test
	public void testRequestMonitorNonMvcRequestDoMonitor() throws Exception {
		when(configuration.getBoolean(SpringMvcPlugin.MONITOR_ONLY_SPRING_MVC_REQUESTS)).thenReturn(false);

		final SpringMonitoredHttpRequest monitoredRequest = createSpringMonitoredHttpRequest(nonMvcRequest);
		registerAspect(monitoredRequest, nonMvcRequest, null);
		RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation = requestMonitor.monitor(monitoredRequest);

		assertEquals(1, requestInformation.getRequestTimer().getCount());
		assertEquals("GET-*:js", requestInformation.getTimerName());
		assertEquals("GET *.js", requestInformation.getRequestTrace().getName());
		assertNotNull(getMetricRegistry().getTimers().get(name("request", "GET-*:js", "server", "time", "total")));
		verify(monitoredRequest, times(1)).onPostExecute(anyRequestInformation());
		verify(monitoredRequest, times(useNameDeterminerAspect ? 0 : 1)).getRequestName();
	}

	@Test
	public void testRequestMonitorNonMvcRequestDontMonitor() throws Exception {
		when(configuration.getBoolean(SpringMvcPlugin.MONITOR_ONLY_SPRING_MVC_REQUESTS)).thenReturn(true);

		final SpringMonitoredHttpRequest monitoredRequest = createSpringMonitoredHttpRequest(nonMvcRequest);
		registerAspect(monitoredRequest, nonMvcRequest, null);
		RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation = requestMonitor.monitor(monitoredRequest);

		Assert.assertEquals("", requestInformation.getRequestTrace().getName());
		assertNull(getMetricRegistry().getTimers().get(name("request", sanitizeGraphiteMetricSegment("GET *.js"), "server", "time", "total")));
		verify(monitoredRequest, never()).onPostExecute(anyRequestInformation());
		verify(monitoredRequest, times(useNameDeterminerAspect ? 0 : 1)).getRequestName();
	}

	private void registerAspect(SpringMonitoredHttpRequest monitoredRequest, final HttpServletRequest request, final HandlerExecutionChain mapping) throws Exception {
		if (useNameDeterminerAspect) {
			when(monitoredRequest.execute()).thenAnswer(new Answer<Object>() {
				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					springMVCRequestNameDeterminerAspect.aroundGetHandler(request, mapping);
					return null;
				}
			});
		}
	}

	private RequestMonitor.RequestInformation<HttpRequestTrace> anyRequestInformation() {
		return any();
	}

	private SpringMonitoredHttpRequest createSpringMonitoredHttpRequest(HttpServletRequest request) throws IOException {
		final StatusExposingByteCountingServletResponse response = new StatusExposingByteCountingServletResponse(new MockHttpServletResponse());
		return Mockito.spy(new SpringMonitoredHttpRequest(request, response, new MockFilterChain(), configuration, handlerMappings));
	}
}
