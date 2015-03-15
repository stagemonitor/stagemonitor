package org.stagemonitor.web.monitor;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

public class MonitoredHttpRequestTest {

	private MonitoredHttpRequest monitoredHttpRequest;

	@Before
	public void setUp() throws Exception {
		Stagemonitor.startMonitoring(new MeasurementSession("MonitoredHttpRequestTest", "testHost", "testInstance")).get();
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test.js");
		request.addParameter("foo", "bar");
		request.addParameter("bla", "blubb");
		request.addParameter("pwd", "secret");
		request.addParameter("creditCard", "123456789");
		request.addHeader("Cookie", "foobar");
		request.addHeader("accept", "application/json");

		monitoredHttpRequest = new MonitoredHttpRequest(
				request,
				new StatusExposingByteCountingServletResponse(new MockHttpServletResponse()),
				new MockFilterChain(),
				Stagemonitor.getConfiguration());
	}

	@After
	public void tearDown() throws Exception {
		Stagemonitor.reset();
	}

	@Test
	public void testGetRequestName() throws Exception {
		assertEquals("GET *.js", monitoredHttpRequest.getRequestName());
		testGetInstanceName();
	}

	@Test
	public void testGetInstanceName() {
		assertEquals("localhost", monitoredHttpRequest.getInstanceName());
	}

	@Test
	public void testIsMonitorForwardedExecutions() throws Exception {
		assertEquals(true, monitoredHttpRequest.isMonitorForwardedExecutions());

	}

	@Test
	public void testCreateRequestTrace() throws Exception {
		final HttpRequestTrace requestTrace = monitoredHttpRequest.createRequestTrace();
		assertNull(requestTrace.getParameter());
		assertEquals("/test.js", requestTrace.getUrl());
		assertEquals("GET *.js", requestTrace.getName());
		assertEquals("GET", requestTrace.getMethod());
		assertEquals("GET", requestTrace.getMethod());
		assertNotNull(requestTrace.getId());
		assertNotNull(requestTrace.getTimestamp());
		assertTrue("Timestamp should be in format yyyy-MM-dd'T'HH:mm:ss.SSSZ", requestTrace.getTimestamp().contains("T"));

		assertEquals("MonitoredHttpRequestTest", requestTrace.getApplication());
		assertEquals("testHost", requestTrace.getHost());
		assertEquals("testInstance", requestTrace.getInstance());

		assertEquals(new HashSet<String>(asList("accept")), requestTrace.getHeaders().keySet());
		assertFalse(requestTrace.getHeaders().containsKey("cookie"));
		assertFalse(requestTrace.getHeaders().containsKey("Cookie"));

		final RequestMonitor.RequestInformation requestInformation = mock(RequestMonitor.RequestInformation.class);
		when(requestInformation.getRequestTrace()).thenReturn(requestTrace);
		monitoredHttpRequest.onPostExecute(requestInformation);
		assertEquals("?foo=bar&bla=blubb&pwd=XXXX&creditCard=XXXX", requestTrace.getParameter());

	}
}
