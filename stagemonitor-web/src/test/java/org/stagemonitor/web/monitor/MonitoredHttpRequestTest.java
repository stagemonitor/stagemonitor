package org.stagemonitor.web.monitor;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

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

	@Before
	public void setUp() throws Exception {
		Stagemonitor.reset();
		Stagemonitor.startMonitoring(new MeasurementSession("MonitoredHttpRequestTest", "testHost", "testInstance")).get();
	}

	@After
	public void tearDown() throws Exception {
		Stagemonitor.reset();
	}

	@Test
	public void testGetRequestName() throws Exception {
		final MonitoredHttpRequest monitoredHttpRequest = createMonitoredHttpRequest(new MockHttpServletRequest("GET", "/test.js"));
		assertEquals("GET *.js", monitoredHttpRequest.getRequestName());
		testGetInstanceName();
	}

	@Test
	public void testGetInstanceName() throws Exception {
		final MonitoredHttpRequest monitoredHttpRequest = createMonitoredHttpRequest(new MockHttpServletRequest("GET", "/test.js"));
		assertEquals("localhost", monitoredHttpRequest.getInstanceName());
	}

	@Test
	public void testIsMonitorForwardedExecutions() throws Exception {
		final MonitoredHttpRequest monitoredHttpRequest = createMonitoredHttpRequest(new MockHttpServletRequest("GET", "/test.js"));
		assertEquals(true, monitoredHttpRequest.isMonitorForwardedExecutions());
	}

	@Test
	public void testCreateRequestTrace() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test.js");
		request.addParameter("foo", "bar");
		request.addParameter("bla", "blubb");
		request.addParameter("pwd", "secret");
		request.addParameter("creditCard", "123456789");
		request.addHeader("Cookie", "foobar");
		request.addHeader("accept", "application/json");

		final MonitoredHttpRequest monitoredHttpRequest = createMonitoredHttpRequest(request);

		final HttpRequestTrace requestTrace = monitoredHttpRequest.createRequestTrace();
		assertNull(requestTrace.getParameters());
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
		when(requestInformation.getRequestName()).thenReturn(requestTrace.getName());
		monitoredHttpRequest.onPostExecute(requestInformation);
		final Map<String, String> parameters = requestTrace.getParameters();
		assertEquals("bar", parameters.get("foo"));
		assertEquals("blubb", parameters.get("bla"));
		assertEquals("XXXX", parameters.get("pwd"));
		assertEquals("XXXX", parameters.get("creditCard"));
	}

	@Test
	public void testReferringSite() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test.js");
		request.addHeader("Referer", "https://www.github.com/stagemonitor/stagemonitor");

		final MonitoredHttpRequest monitoredHttpRequest = createMonitoredHttpRequest(request);

		final HttpRequestTrace requestTrace = monitoredHttpRequest.createRequestTrace();
		assertEquals("www.github.com", requestTrace.getReferringSite());
	}

	@Test
	public void testReferringSameHostSite() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test.js");
		request.addHeader("Referer", "https://www.myapp.com:8080/categories");
		request.setServerName("www.myapp.com");

		final MonitoredHttpRequest monitoredHttpRequest = createMonitoredHttpRequest(request);

		final HttpRequestTrace requestTrace = monitoredHttpRequest.createRequestTrace();
		assertNull(requestTrace.getReferringSite());
	}

	private MonitoredHttpRequest createMonitoredHttpRequest(MockHttpServletRequest request) throws IOException {
		return new MonitoredHttpRequest(
				request,
				new StatusExposingByteCountingServletResponse(new MockHttpServletResponse()),
				new MockFilterChain(),
				Stagemonitor.getConfiguration());
	}
}
