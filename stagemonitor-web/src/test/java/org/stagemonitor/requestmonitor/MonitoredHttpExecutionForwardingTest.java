package org.stagemonitor.requestmonitor;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.Metric2Filter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.web.monitor.MonitoredHttpRequest;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import io.opentracing.tag.Tags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class MonitoredHttpExecutionForwardingTest {

	private RequestMonitor.RequestInformation requestInformation1;
	private RequestMonitor.RequestInformation requestInformation2;
	private RequestMonitor.RequestInformation requestInformation3;
	private MonitoredHttpExecutionForwardingTest.TestObject testObject;
	private Metric2Registry metricRegistry;

	@Before
	public void clearState() {
		Stagemonitor.reset();
		Stagemonitor.getMetric2Registry().removeMatching(Metric2Filter.ALL);
		Stagemonitor.startMonitoring(new MeasurementSession("MonitoredHttpExecutionForwardingTest", "testHost", "testInstance"));
		requestInformation1 = requestInformation2 = requestInformation3 = null;
		metricRegistry = Stagemonitor.getMetric2Registry();
		testObject = new TestObject(new RequestMonitor(Stagemonitor.getConfiguration(), metricRegistry));
	}

	@Test
	public void testForwarding() throws Exception {
		TestObject testObject = this.testObject;
		testObject.monitored1();
		assertFalse(requestInformation1.isForwarded());
		assertTrue(requestInformation2.isForwarded());
		assertTrue(requestInformation3.isForwarded());

		assertEquals("/monitored3", requestInformation3.getInternalSpan().getTags().get(Tags.HTTP_URL.getKey()));
		assertEquals("GET /monitored3", requestInformation3.getInternalSpan().getOperationName());

		assertNull(metricRegistry.getTimers().get(name("response_time_server").tag("request_name", "GET /monitored1").layer("All").build()));
		assertNull(metricRegistry.getTimers().get(name("response_time_server").tag("request_name", "GET /monitored2").layer("All").build()));
		assertNotNull(metricRegistry.getTimers().get(name("response_time_server").tag("request_name", "GET /monitored3").layer("All").build()));
	}

	@Test
	public void testNoForwarding() throws Exception {
		testObject.monitored3();
		assertNull(requestInformation1);
		assertNull(requestInformation2);
		assertFalse(requestInformation3.isForwarded());

		assertEquals("/monitored3", requestInformation3.getInternalSpan().getTags().get(Tags.HTTP_URL.getKey()));
		assertEquals("GET /monitored3", requestInformation3.getInternalSpan().getOperationName());

		assertNull(metricRegistry.getTimers().get(name("response_time_server").tag("request_name", "GET /monitored1").layer("All").build()));
		assertNull(metricRegistry.getTimers().get(name("response_time_server").tag("request_name", "GET /monitored2").layer("All").build()));
		assertNotNull(metricRegistry.getTimers().get(name("response_time_server").tag("request_name", "GET /monitored3").layer("All").build()));
	}

	private class TestObject {
		private final RequestMonitor requestMonitor;

		private TestObject(RequestMonitor requestMonitor) {
			this.requestMonitor = requestMonitor;
		}

		private void monitored1() throws Exception {
			requestInformation1 = requestMonitor.monitor(new MonitoredHttpRequest(new MockHttpServletRequest("GET", "/monitored1"),
					new StatusExposingByteCountingServletResponse(new MockHttpServletResponse()),
					new FilterChain() {
						@Override
						public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
							try {
								monitored2();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}, Stagemonitor.getConfiguration()));
		}

		private void monitored2() throws Exception {
			requestInformation2 = requestMonitor.monitor(new MonitoredHttpRequest(new MockHttpServletRequest("GET", "/monitored2"),
					new StatusExposingByteCountingServletResponse(new MockHttpServletResponse()),
					new FilterChain() {
						@Override
						public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
							try {
								monitored3();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}, Stagemonitor.getConfiguration()));
		}

		private void monitored3() throws Exception {
			requestInformation3 = requestMonitor.monitor(new MonitoredHttpRequest(new MockHttpServletRequest("GET", "/monitored3"),
					new StatusExposingByteCountingServletResponse(new MockHttpServletResponse()),
					new FilterChain() {
						@Override
						public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
							// actual work
						}
					}, Stagemonitor.getConfiguration()));
		}

	}

}
