package org.stagemonitor.web.monitor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.stagemonitor.core.Configuration;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpringMonitoredHttpExecutionTest {

	private MockHttpServletRequest mvcRequest = new MockHttpServletRequest("GET", "/test/requestName");
	private MockHttpServletRequest nonMvcRequest = new MockHttpServletRequest("GET", "/static/jquery.js");
	private List<HandlerMapping> handlerMappings;

	@Before
	public void before() throws Exception {
		// the purpose of this class is to obtain a instance to a Method, because Method objects can't be mocked as they are final
		class Test { public void testGetRequestName() {} }
		handlerMappings = Arrays.asList(
				createExceptionThrowingHandlerMapping(),
				createHandlerMappingNotReturningHandlerMethod(),
				createHandlerMapping(mvcRequest, Test.class.getMethod("testGetRequestName"))
		);
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
	public void testGetRequestNameWithMvcMapping() throws Exception {
		Configuration configuration = mock(Configuration.class);
		when(configuration.isMonitorOnlySpringMvcRequests()).thenReturn(false);
		SpringMonitoredHttpExecution springMonitoredHttpExecution = new SpringMonitoredHttpExecution(mvcRequest, null,
				null, configuration, handlerMappings);

		assertEquals("Test Get Request Name", springMonitoredHttpExecution.getRequestName());
	}

	@Test
	public void testGetRequestNameWithoutMvcMappingWithFallback() throws Exception {
		Configuration configuration = mock(Configuration.class);
		when(configuration.isMonitorOnlySpringMvcRequests()).thenReturn(false);
		SpringMonitoredHttpExecution springMonitoredHttpExecution = new SpringMonitoredHttpExecution(nonMvcRequest,
				null, null, configuration, handlerMappings);

		assertEquals("GET /static/jquery.js", springMonitoredHttpExecution.getRequestName());
	}

	@Test
	public void testGetRequestNameWithoutMvcMappingWithoutFallback() throws Exception {
		Configuration configuration = mock(Configuration.class);
		when(configuration.isMonitorOnlySpringMvcRequests()).thenReturn(true);
		SpringMonitoredHttpExecution springMonitoredHttpExecution = new SpringMonitoredHttpExecution(nonMvcRequest,
				null, null, configuration, handlerMappings);

		Assert.assertNull(springMonitoredHttpExecution.getRequestName());
	}
}
