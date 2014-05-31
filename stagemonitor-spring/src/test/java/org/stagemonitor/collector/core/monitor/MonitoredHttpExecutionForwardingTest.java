package org.stagemonitor.collector.core.monitor;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.collector.core.StageMonitor;
import org.stagemonitor.collector.web.monitor.HttpExecutionContext;
import org.stagemonitor.collector.web.monitor.MonitoredHttpExecution;
import org.stagemonitor.collector.web.monitor.filter.StatusExposingByteCountingServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

import static com.codahale.metrics.MetricRegistry.name;
import static org.junit.Assert.*;
import static org.stagemonitor.collector.core.StageMonitor.getMetricRegistry;
import static org.stagemonitor.collector.core.util.GraphiteEncoder.encodeForGraphite;

public class MonitoredHttpExecutionForwardingTest {

	private ExecutionContextMonitor.ExecutionInformation<HttpExecutionContext> executionInformation1;
	private ExecutionContextMonitor.ExecutionInformation<HttpExecutionContext> executionInformation2;
	private ExecutionContextMonitor.ExecutionInformation<HttpExecutionContext> executionInformation3;

	@Before
	public void clearState() {
		getMetricRegistry().removeMatching(new MetricFilter() {
			@Override
			public boolean matches(String name, Metric metric) {
				return true;
			}
		});
		executionInformation1 = executionInformation2 = executionInformation3 = null;
	}

	@Test
	public void testForwarding() throws Exception {
		TestObject testObject = new TestObject(new ExecutionContextMonitor());
		testObject.monitored1();
		assertFalse(executionInformation1.forwardedExecution);
		assertTrue(executionInformation2.forwardedExecution);
		assertTrue(executionInformation3.forwardedExecution);

		assertEquals("/monitored3", executionInformation3.executionContext.getUrl());
		assertEquals("GET /monitored3", executionInformation3.executionContext.getName());

		assertNull(getMetricRegistry().getTimers().get(name("request", "total", encodeForGraphite("GET /monitored1"))));
		assertNull(getMetricRegistry().getTimers().get(name("request", "total", encodeForGraphite("GET /monitored2"))));
		assertNotNull(getMetricRegistry().getTimers().get(name("request", "total", encodeForGraphite("GET /monitored3"))));
	}

	@Test
	public void testNoForwarding() throws Exception {
		TestObject testObject = new TestObject(new ExecutionContextMonitor());
		testObject.monitored3();
		assertNull(executionInformation1);
		assertNull(executionInformation2);
		assertFalse(executionInformation3.forwardedExecution);

		assertEquals("/monitored3", executionInformation3.executionContext.getUrl());
		assertEquals("GET /monitored3", executionInformation3.executionContext.getName());

		assertNull(getMetricRegistry().getTimers().get(name("request", "total", encodeForGraphite("GET /monitored1"))));
		assertNull(getMetricRegistry().getTimers().get(name("request", "total", encodeForGraphite("GET /monitored2"))));
		assertNotNull(getMetricRegistry().getTimers().get(name("request", "total", encodeForGraphite("GET /monitored3"))));
	}


	private class TestObject {
		private final ExecutionContextMonitor executionContextMonitor;

		private TestObject(ExecutionContextMonitor executionContextMonitor) {
			this.executionContextMonitor = executionContextMonitor;
		}

		private void monitored1() throws Exception {
			executionInformation1 = executionContextMonitor.monitor(new MonitoredHttpExecution(new MockHttpServletRequest("GET", "/monitored1"),
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
					}, StageMonitor.getConfiguration()));
		}

		private void monitored2() throws Exception {
			executionInformation2 = executionContextMonitor.monitor(new MonitoredHttpExecution(new MockHttpServletRequest("GET", "/monitored2"),
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
					}, StageMonitor.getConfiguration()));
		}

		private void monitored3() throws Exception {
			executionInformation3 = executionContextMonitor.monitor(new MonitoredHttpExecution(new MockHttpServletRequest("GET", "/monitored3"),
					new StatusExposingByteCountingServletResponse(new MockHttpServletResponse()),
					new FilterChain() {
						@Override
						public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
							// actual work
						}
					}, StageMonitor.getConfiguration()));
		}

	}

}
