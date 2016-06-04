package org.stagemonitor.web.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.web.WebPlugin;

public class StagemonitorMetricsServletTest {

	private StagemonitorMetricsServlet servlet;
	private Metric2Registry registry;

	@Before
	public void setUp() throws Exception {
		registry = new Metric2Registry();
		servlet = new StagemonitorMetricsServlet(registry, mock(WebPlugin.class), JsonUtils.getMapper());
	}

	@Test
	public void getAllMetrics() throws Exception {
		registry.counter(name("foo").tag("bar", "baz").build()).inc();
		registry.counter(name("qux").tag("quux", "foo").build()).inc();
		final MockHttpServletResponse resp = new MockHttpServletResponse();
		servlet.doGet(new MockHttpServletRequest(), resp);
		assertEquals("[{\"name\":\"qux\",\"quux\":\"foo\",\"count\":1},{\"name\":\"foo\",\"bar\":\"baz\",\"count\":1}]", resp.getContentAsString());
	}

	@Test
	public void getFilteredMetrics() throws Exception {
		registry.counter(name("foo").tag("bar", "baz").build()).inc();
		registry.counter(name("qux").tag("quux", "foo").build()).inc();
		final MockHttpServletResponse resp = new MockHttpServletResponse();
		final MockHttpServletRequest req = new MockHttpServletRequest();
		req.addParameter("metricNames[]", "foo");
		servlet.doGet(req, resp);
		assertEquals("[{\"name\":\"foo\",\"bar\":\"baz\",\"count\":1}]", resp.getContentAsString());
	}

	@Test
	public void getMeter() throws Exception {
		registry.meter(name("foo").tag("bar", "baz").build()).mark();
		final MockHttpServletResponse resp = new MockHttpServletResponse();
		servlet.doGet(new MockHttpServletRequest(), resp);
		final double m1_rate = JsonUtils.getMapper().readTree(resp.getContentAsString()).get(0).get("m1_rate").doubleValue();
		assertTrue("Expected m1 rate of > 0, but got " + m1_rate, m1_rate > 0);
	}

}