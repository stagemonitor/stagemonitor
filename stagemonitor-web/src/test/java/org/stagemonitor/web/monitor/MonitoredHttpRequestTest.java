package org.stagemonitor.web.monitor;

import com.uber.jaeger.Span;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.utils.SpanUtils;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

import java.io.IOException;

import io.opentracing.tag.Tags;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MonitoredHttpRequestTest {

	@Before
	public void setUp() throws Exception {
		Stagemonitor.reset();
		Stagemonitor.startMonitoring(new MeasurementSession("MonitoredHttpRequestTest", "testHost", "testInstance"));
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
		request.addHeader("Accept", "application/json");

		final MonitoredHttpRequest monitoredHttpRequest = createMonitoredHttpRequest(request);

		final Span span = SpanUtils.getInternalSpan(monitoredHttpRequest.createSpan());
		assertEquals("/test.js", span.getTags().get(Tags.HTTP_URL.getKey()));
		assertEquals("GET *.js", span.getOperationName());
		assertEquals("GET", span.getTags().get("method"));
		assertNotNull(span.context().getSpanID());
		assertNotNull(span.getStart());

		assertEquals("application/json", span.getTags().get(SpanUtils.HTTP_HEADERS_PREFIX + "accept"));
		assertFalse(span.getTags().containsKey(SpanUtils.HTTP_HEADERS_PREFIX + "cookie"));
		assertFalse(span.getTags().containsKey(SpanUtils.HTTP_HEADERS_PREFIX + "Cookie"));

		final RequestMonitor.RequestInformation requestInformation = mock(RequestMonitor.RequestInformation.class);
		when(requestInformation.getSpan()).thenReturn(span);
		when(requestInformation.getRequestName()).thenReturn(span.getOperationName());
		monitoredHttpRequest.onPostExecute(requestInformation);
		assertEquals("bar", span.getTags().get(SpanUtils.PARAMETERS_PREFIX + "foo"));
		assertEquals("blubb", span.getTags().get(SpanUtils.PARAMETERS_PREFIX + "bla"));
		assertEquals("XXXX", span.getTags().get(SpanUtils.PARAMETERS_PREFIX + "pwd"));
		assertEquals("XXXX", span.getTags().get(SpanUtils.PARAMETERS_PREFIX + "creditCard"));
	}

	@Test
	public void testReferringSite() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test.js");
		request.addHeader("Referer", "https://www.github.com/stagemonitor/stagemonitor");

		final MonitoredHttpRequest monitoredHttpRequest = createMonitoredHttpRequest(request);

		final Span span = SpanUtils.getInternalSpan(monitoredHttpRequest.createSpan());
		assertEquals("www.github.com", span.getTags().get("http.referring_site"));
	}

	@Test
	public void testReferringSameHostSite() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test.js");
		request.addHeader("Referer", "https://www.myapp.com:8080/categories");
		request.setServerName("www.myapp.com");

		final MonitoredHttpRequest monitoredHttpRequest = createMonitoredHttpRequest(request);

		final Span span = SpanUtils.getInternalSpan(monitoredHttpRequest.createSpan());
		assertNull(span.getTags().get("http.referring_site"));
	}

	private MonitoredHttpRequest createMonitoredHttpRequest(MockHttpServletRequest request) throws IOException {
		return new MonitoredHttpRequest(
				request,
				new StatusExposingByteCountingServletResponse(new MockHttpServletResponse()),
				new MockFilterChain(),
				Stagemonitor.getConfiguration());
	}
}
