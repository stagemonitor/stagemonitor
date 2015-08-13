package org.stagemonitor.web.monitor.resteasy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;
import static org.stagemonitor.requestmonitor.BusinessTransactionNamingStrategy.CLASS_NAME_DOT_METHOD_NAME;
import static org.stagemonitor.requestmonitor.BusinessTransactionNamingStrategy.CLASS_NAME_HASH_METHOD_NAME;
import static org.stagemonitor.requestmonitor.BusinessTransactionNamingStrategy.METHOD_NAME_SPLIT_CAMEL_CASE;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.Registry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.HttpRequestTrace;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

@RunWith(value = Parameterized.class)
public class ResteasyRequestMonitorTest {
    private MockHttpServletRequest resteasyServletRequest = new MockHttpServletRequest("GET", "/test/requestName");
    private MockHttpServletRequest nonResteasyServletRequest = new MockHttpServletRequest("GET", "/META-INF/resources/stagemonitor/static/jquery.js");
    private MockHttpRequest resteasyRequest;
    private Configuration configuration = mock(Configuration.class);
    private RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
    private WebPlugin webPlugin = mock(WebPlugin.class);
    private CorePlugin corePlugin = mock(CorePlugin.class);
    private RequestMonitor requestMonitor;
    private Metric2Registry registry = new Metric2Registry();
    private final boolean useNameDeterminerAspect;
    private Registry getRequestNameRegistry;

    public ResteasyRequestMonitorTest(boolean useNameDeterminerAspect) {
        this.useNameDeterminerAspect = useNameDeterminerAspect;
    }

    // the purpose of this class is to obtain a instance to a Method,
    // because Method objects can't be mocked as they are final
    private static class TestResource { public void testGetRequestName() {} }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] { { true }, { false } };
        return Arrays.asList(data);
    }

    @Before
    public void before() throws Exception {
        resteasyRequest = MockHttpRequest.create(resteasyServletRequest.getMethod(), resteasyServletRequest.getRequestURI());
        getRequestNameRegistry = createRegistry(resteasyRequest, TestResource.class.getMethod("testGetRequestName"));
        resteasyServletRequest.getServletContext().setAttribute(Registry.class.getName(), getRequestNameRegistry);
        nonResteasyServletRequest.getServletContext().setAttribute(Registry.class.getName(), getRequestNameRegistry);
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
        when(requestMonitorPlugin.isCollectRequestStats()).thenReturn(true);
        when(requestMonitorPlugin.getBusinessTransactionNamingStrategy()).thenReturn(METHOD_NAME_SPLIT_CAMEL_CASE);
        when(webPlugin.getGroupUrls()).thenReturn(Collections.singletonMap(Pattern.compile("(.*).js$"), "*.js"));
        requestMonitor = new RequestMonitor(corePlugin, registry, requestMonitorPlugin);
        ResteasyRequestNameDeterminerInstrumenter.setWebPlugin(webPlugin);
        ResteasyRequestNameDeterminerInstrumenter.setRequestMonitorPlugin(requestMonitorPlugin);
    }

    private Registry createRegistry(final MockHttpRequest request, Method requestMappingMethod) {
        ResourceInvoker invoker = mock(ResourceMethodInvoker.class);
        when(invoker.getMethod()).thenReturn(requestMappingMethod);

        ArgumentMatcher<HttpRequest> httpRequestMatcher = new ArgumentMatcher<HttpRequest>() {
            @Override
            public boolean matches(Object argument) {
                if (argument == null) {
                    return false;
                }

                if (!HttpRequest.class.isAssignableFrom(argument.getClass())) {
                    return false;
                }

                HttpRequest other = (HttpRequest) argument;
                return request.getUri().getPath().equals(other.getUri().getPath())
                        && request.getHttpMethod().equals(other.getHttpMethod());
            }
        };

        Registry registry = mock(Registry.class);
        when(registry.getResourceInvoker(argThat(httpRequestMatcher))).thenReturn(invoker);
        return registry;
    }

    @Test
    public void testRequestMonitorResteasyRequest() throws Exception {
        System.out.println("useNameDeterminerAspect="+useNameDeterminerAspect);
        when(webPlugin.isMonitorOnlyResteasyRequests()).thenReturn(false);

        ResteasyMonitoredHttpRequest monitoredRequest = createResteasyMonitoredHttpRequest(resteasyServletRequest);
        registerAspect(monitoredRequest, getRequestNameRegistry.getResourceInvoker(resteasyRequest));
        final RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation = requestMonitor.monitor(monitoredRequest);

        assertEquals(1, requestInformation.getRequestTimer().getCount());
        assertEquals("Test Get Request Name", requestInformation.getRequestName());
        assertEquals("Test Get Request Name", requestInformation.getRequestTrace().getName());
        assertEquals("/test/requestName", requestInformation.getRequestTrace().getUrl());
        assertEquals(Integer.valueOf(200), requestInformation.getRequestTrace().getStatusCode());
        assertEquals("GET", requestInformation.getRequestTrace().getMethod());
        Assert.assertNull(requestInformation.getExecutionResult());
        assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", "Test Get Request Name").tier("server").layer("total").build()));
        verify(monitoredRequest, times(1)).onPostExecute(anyRequestInformation());
        verify(monitoredRequest, times(useNameDeterminerAspect ? 0 : 1)).getRequestName();
    }

    @Test
    public void testRequestMonitorResteasyRequestWithClassHashMethodNaming() throws Exception {
        System.out.println("useNameDeterminerAspect="+useNameDeterminerAspect);
        when(webPlugin.isMonitorOnlyResteasyRequests()).thenReturn(false);
        when(requestMonitorPlugin.getBusinessTransactionNamingStrategy()).thenReturn(CLASS_NAME_HASH_METHOD_NAME);

        ResteasyMonitoredHttpRequest monitoredRequest = createResteasyMonitoredHttpRequest(resteasyServletRequest);
        registerAspect(monitoredRequest, getRequestNameRegistry.getResourceInvoker(resteasyRequest));
        final RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation = requestMonitor.monitor(monitoredRequest);

        assertEquals(1, requestInformation.getRequestTimer().getCount());
        assertEquals("TestResource#testGetRequestName", requestInformation.getRequestName());
        assertEquals("TestResource#testGetRequestName", requestInformation.getRequestTrace().getName());
        assertEquals("/test/requestName", requestInformation.getRequestTrace().getUrl());
        assertEquals(Integer.valueOf(200), requestInformation.getRequestTrace().getStatusCode());
        assertEquals("GET", requestInformation.getRequestTrace().getMethod());
        Assert.assertNull(requestInformation.getExecutionResult());
        assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", "TestResource#testGetRequestName").tier("server").layer("total").build()));
        verify(monitoredRequest, times(1)).onPostExecute(anyRequestInformation());
        verify(monitoredRequest, times(useNameDeterminerAspect ? 0 : 1)).getRequestName();
    }

    @Test
    public void testRequestMonitorResteasyRequestWithClassDotMethodNaming() throws Exception {
        System.out.println("useNameDeterminerAspect="+useNameDeterminerAspect);
        when(webPlugin.isMonitorOnlyResteasyRequests()).thenReturn(false);
        when(requestMonitorPlugin.getBusinessTransactionNamingStrategy()).thenReturn(CLASS_NAME_DOT_METHOD_NAME);

        ResteasyMonitoredHttpRequest monitoredRequest = createResteasyMonitoredHttpRequest(resteasyServletRequest);
        registerAspect(monitoredRequest, getRequestNameRegistry.getResourceInvoker(resteasyRequest));
        final RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation = requestMonitor.monitor(monitoredRequest);

        assertEquals(1, requestInformation.getRequestTimer().getCount());
        assertEquals("TestResource.testGetRequestName", requestInformation.getRequestName());
        assertEquals("TestResource.testGetRequestName", requestInformation.getRequestTrace().getName());
        assertEquals("/test/requestName", requestInformation.getRequestTrace().getUrl());
        assertEquals(Integer.valueOf(200), requestInformation.getRequestTrace().getStatusCode());
        assertEquals("GET", requestInformation.getRequestTrace().getMethod());
        Assert.assertNull(requestInformation.getExecutionResult());
        assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", "TestResource.testGetRequestName").tier("server").layer("total").build()));
        verify(monitoredRequest, times(1)).onPostExecute(anyRequestInformation());
        verify(monitoredRequest, times(useNameDeterminerAspect ? 0 : 1)).getRequestName();
    }

    @Test
    public void testRequestMonitorNonResteasyRequestDoMonitor() throws Exception {
        when(webPlugin.isMonitorOnlyResteasyRequests()).thenReturn(false);

        ResteasyMonitoredHttpRequest monitoredRequest = createResteasyMonitoredHttpRequest(nonResteasyServletRequest);
        registerAspect(monitoredRequest, getRequestNameRegistry.getResourceInvoker(resteasyRequest));
        registerAspect(monitoredRequest, null);
        RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation = requestMonitor.monitor(monitoredRequest);

        assertEquals(1, requestInformation.getRequestTimer().getCount());
        assertEquals("GET *.js", requestInformation.getRequestName());
        assertEquals("GET *.js", requestInformation.getRequestTrace().getName());
        assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", "GET *.js").tier("server").layer("total").build()));
        verify(monitoredRequest, times(1)).onPostExecute(anyRequestInformation());
        verify(monitoredRequest, times(1)).getRequestName();
    }

    @Test
    public void testRequestMonitorNonResteasyRequestDontMonitor() throws Exception {
        when(webPlugin.isMonitorOnlyResteasyRequests()).thenReturn(true);

        ResteasyMonitoredHttpRequest monitoredRequest = createResteasyMonitoredHttpRequest(nonResteasyServletRequest);
        registerAspect(monitoredRequest, getRequestNameRegistry.getResourceInvoker(resteasyRequest));
        registerAspect(monitoredRequest, null);
        RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation = requestMonitor.monitor(monitoredRequest);

        assertEquals("", requestInformation.getRequestTrace().getName());
        assertNull(registry.getTimers().get(name("response_time").tag("request_name", "GET *.js").tier("server").layer("total").build()));
        verify(monitoredRequest, never()).onPostExecute(anyRequestInformation());
        verify(monitoredRequest, times(useNameDeterminerAspect ? 0 : 1)).getRequestName();
    }

    private void registerAspect(ResteasyMonitoredHttpRequest monitoredRequest, final ResourceInvoker invoker) throws Exception {
        if (useNameDeterminerAspect) {
            when(monitoredRequest.execute()).thenAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    ResteasyRequestNameDeterminerInstrumenter.setRequestNameByInvoker(invoker);
                    return null;
                }
            });
        }
    }

    private RequestMonitor.RequestInformation<HttpRequestTrace> anyRequestInformation() {
        return any();
    }

    private ResteasyMonitoredHttpRequest createResteasyMonitoredHttpRequest(HttpServletRequest request) throws IOException {
        final StatusExposingByteCountingServletResponse response = new StatusExposingByteCountingServletResponse(new MockHttpServletResponse());
        return Mockito.spy(new ResteasyMonitoredHttpRequest(request, response, new MockFilterChain(), configuration));
    }
}
