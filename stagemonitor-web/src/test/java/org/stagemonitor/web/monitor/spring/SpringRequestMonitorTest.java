package org.stagemonitor.web.monitor.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.runners.Parameterized.Parameters;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;
import static org.stagemonitor.requestmonitor.BusinessTransactionNamingStrategy.METHOD_NAME_SPLIT_CAMEL_CASE;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.HttpRequestTrace;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

@RunWith(value = Parameterized.class)
public class SpringRequestMonitorTest {

	private MockHttpServletRequest mvcRequest = new MockHttpServletRequest("GET", "/test/requestName");
	private MockHttpServletRequest nonMvcRequest = new MockHttpServletRequest("GET", "/META-INF/resources/stagemonitor/static/jquery.js");
	private Configuration configuration = mock(Configuration.class);
	private RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
	private WebPlugin webPlugin = mock(WebPlugin.class);
	private CorePlugin corePlugin = mock(CorePlugin.class);
	private RequestMonitor requestMonitor;
	private Metric2Registry registry = new Metric2Registry();
	private final boolean useNameDeterminerAspect;
	private HandlerMapping getRequestNameHandlerMapping;

	public SpringRequestMonitorTest(boolean useNameDeterminerAspect) {
		this.useNameDeterminerAspect = useNameDeterminerAspect;
	}

	// the purpose of this class is to obtain a instance to a Method,
	// because Method objects can't be mocked as they are final
	private static class TestController { public void testGetRequestName() {} }

	@Parameters
	public static Collection<Object[]> data() {
		Object[][] data = new Object[][] { /*{ true },*/ { false } };
		return Arrays.asList(data);
	}

	@Before
	public void before() throws Exception {
		getRequestNameHandlerMapping = createHandlerMapping(mvcRequest, TestController.class.getMethod("testGetRequestName"));
		List<HandlerMapping> handlerMappings = Arrays.asList(
				createExceptionThrowingHandlerMapping(),
				createHandlerMappingNotReturningHandlerMethod(),
				getRequestNameHandlerMapping
		);
		SpringMonitoredHttpRequest.HandlerMappingsExtractor.setAllHandlerMappings(handlerMappings);
		registry.removeMatching(new MetricFilter() {
			@Override
			public boolean matches(String name, Metric metric) {
				return true;
			}
		});
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(configuration.getConfig(WebPlugin.class)).thenReturn(webPlugin);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		when(requestMonitorPlugin.isCollectRequestStats()).thenReturn(true);
		when(requestMonitorPlugin.getBusinessTransactionNamingStrategy()).thenReturn(METHOD_NAME_SPLIT_CAMEL_CASE);
		when(webPlugin.getGroupUrls()).thenReturn(Collections.singletonMap(Pattern.compile("(.*).js$"), "*.js"));
		requestMonitor = new RequestMonitor(corePlugin, registry, requestMonitorPlugin);
		SpringMvcRequestNameDeterminerInstrumenter.setWebPlugin(webPlugin);
	}

	private HandlerMapping createExceptionThrowingHandlerMapping() throws Exception {
		HandlerMapping requestMappingHandlerMapping = mock(HandlerMapping.class);
		when(requestMappingHandlerMapping.getHandler((HttpServletRequest) any())).thenThrow(new Exception("This is a test exeption"));
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
		System.out.println("createHandlerMapping"+request);
		HandlerMapping requestMappingHandlerMapping = mock(HandlerMapping.class);
		HandlerExecutionChain handlerExecutionChain = mock(HandlerExecutionChain.class);
		HandlerMethod handlerMethod = mock(HandlerMethod.class);

		when(handlerMethod.getMethod()).thenReturn(requestMappingMethod);
		doReturn(TestController.class).when(handlerMethod).getBeanType();
		when(handlerExecutionChain.getHandler()).thenReturn(handlerMethod);
		when(requestMappingHandlerMapping.getHandler(Matchers.argThat(new BaseMatcher<HttpServletRequest>() {
			@Override
			public boolean matches(Object item) {
				return ((HttpServletRequest) item).getRequestURI().equals("/test/requestName");
			}

			@Override
			public void describeTo(Description description) {
			}
		}))).thenReturn(handlerExecutionChain);
		return requestMappingHandlerMapping;
	}

	@Test
	public void testRequestMonitorMvcRequest() throws Exception {
		System.out.println("useNameDeterminerAspect="+useNameDeterminerAspect);
		when(webPlugin.isMonitorOnlySpringMvcRequests()).thenReturn(false);

		SpringMonitoredHttpRequest monitoredRequest = createSpringMonitoredHttpRequest(mvcRequest);
		registerAspect(monitoredRequest, mvcRequest, getRequestNameHandlerMapping.getHandler(mvcRequest));
		final RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation = requestMonitor.monitor(monitoredRequest);

		assertEquals(1, requestInformation.getRequestTimer().getCount());
		assertEquals("Test Get Request Name", requestInformation.getRequestName());
		assertEquals("Test Get Request Name", requestInformation.getRequestTrace().getName());
		assertEquals("/test/requestName", requestInformation.getRequestTrace().getUrl());
		assertEquals(Integer.valueOf(200), requestInformation.getRequestTrace().getStatusCode());
		assertEquals("GET", requestInformation.getRequestTrace().getMethod());
		Assert.assertNull(requestInformation.getExecutionResult());
		assertNotNull(registry.getTimers().get(name("response_time_server").tag("request_name", "Test Get Request Name").layer("total").build()));
		verify(monitoredRequest, times(1)).onPostExecute(anyRequestInformation());
		verify(monitoredRequest, times(useNameDeterminerAspect ? 0 : 1)).getRequestName();
	}

	@Test
	public void testRequestMonitorNonMvcRequestDoMonitor() throws Exception {
		when(webPlugin.isMonitorOnlySpringMvcRequests()).thenReturn(false);

		final SpringMonitoredHttpRequest monitoredRequest = createSpringMonitoredHttpRequest(nonMvcRequest);
		registerAspect(monitoredRequest, nonMvcRequest, null);
		RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation = requestMonitor.monitor(monitoredRequest);

		assertEquals(1, requestInformation.getRequestTimer().getCount());
		assertEquals("GET *.js", requestInformation.getRequestName());
		assertEquals("GET *.js", requestInformation.getRequestTrace().getName());
		assertNotNull(registry.getTimers().get(name("response_time_server").tag("request_name", "GET *.js").layer("total").build()));
		verify(monitoredRequest, times(1)).onPostExecute(anyRequestInformation());
		verify(monitoredRequest, times(1)).getRequestName();
	}

	@Test
	public void testRequestMonitorNonMvcRequestDontMonitor() throws Exception {
		when(webPlugin.isMonitorOnlySpringMvcRequests()).thenReturn(true);

		final SpringMonitoredHttpRequest monitoredRequest = createSpringMonitoredHttpRequest(nonMvcRequest);
		registerAspect(monitoredRequest, nonMvcRequest, null);
		RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation = requestMonitor.monitor(monitoredRequest);

		assertEquals("", requestInformation.getRequestTrace().getName());
		assertNull(registry.getTimers().get(name("response_time_server").tag("request_name", "GET *.js").layer("total").build()));
		verify(monitoredRequest, never()).onPostExecute(anyRequestInformation());
		verify(monitoredRequest, times(useNameDeterminerAspect ? 0 : 1)).getRequestName();
	}

	private void registerAspect(SpringMonitoredHttpRequest monitoredRequest, final HttpServletRequest request,
								final HandlerExecutionChain mapping) throws Exception {

		if (useNameDeterminerAspect) {
			when(monitoredRequest.execute()).thenAnswer(new Answer<Object>() {
				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					SpringMvcRequestNameDeterminerInstrumenter.setRequestNameByHandler(mapping);
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
		return Mockito.spy(new SpringMonitoredHttpRequest(request, response, new MockFilterChain(), configuration));
	}
}

