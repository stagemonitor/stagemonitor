package org.stagemonitor.web.rum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.rum.RumServlet;

public class RumServletTest {

	private final Metric2Registry registry = new Metric2Registry();
	private WebPlugin webPlugin = mock(WebPlugin.class);
	private RumServlet rumServlet = new RumServlet(registry, webPlugin);

	@Before
	public void setUp() throws Exception {
		when(webPlugin.isRealUserMonitoringEnabled()).thenReturn(true);
	}

	@Test
	public void testBeaconPerRequest() throws Exception {
		when(webPlugin.isCollectPageLoadTimesPerRequest()).thenReturn(true);
		final MockHttpServletRequest req = new MockHttpServletRequest();
		final String requestName = "GET /test.html";
		req.addParameter("requestName", requestName);
		req.addParameter("serverTime", "100");
		req.addParameter("domProcessing", "10");
		req.addParameter("pageRendering", "30");
		req.addParameter("timeToFirstByte", "160");
		final MockHttpServletResponse resp = new MockHttpServletResponse();

		rumServlet.doGet(req, resp);

		assertEquals(200, resp.getStatus());
		assertEquals("image/png", resp.getContentType());

		assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", requestName).tag("tier", "browser").type("dom_processing").build()));
		assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", requestName).tag("tier", "browser").type("page_rendering").build()));
		assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "browser").type("dom_processing").build()));
		assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "browser").type("page_rendering").build()));
		assertEquals(TimeUnit.MILLISECONDS.toNanos(10), registry.getTimers().get(name("response_time").tag("request_name", requestName).tag("tier", "browser").type("dom_processing").build()).getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(30), registry.getTimers().get(name("response_time").tag("request_name", requestName).tag("tier", "browser").type("page_rendering").build()).getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(10), registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "browser").type("dom_processing").build()).getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(30), registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "browser").type("page_rendering").build()).getSnapshot().getMax());

		assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", requestName).tag("tier", "network").build()));
		assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "network").build()));
		// t_resp-serverTime
		assertEquals(TimeUnit.MILLISECONDS.toNanos(60), registry.getTimers().get(name("response_time").tag("request_name", requestName).tag("tier", "network").build()).getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(60), registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "network").build()).getSnapshot().getMax());

		assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", requestName).tag("tier", "total").build()));
		assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "total").build()));
		// t_page + t_resp
		assertEquals(TimeUnit.MILLISECONDS.toNanos(200), registry.getTimers().get(name("response_time").tag("request_name", requestName).tag("tier", "total").build()).getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(200), registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "total").build()).getSnapshot().getMax());

		assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", requestName).tag("tier", "server_rum").build()));
		assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "server_rum").build()));
		assertEquals(TimeUnit.MILLISECONDS.toNanos(100), registry.getTimers().get(name("response_time").tag("request_name", requestName).tag("tier", "server_rum").build()).getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(100), registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "server_rum").build()).getSnapshot().getMax());
	}

	@Test
	public void testBeaconAggregate() throws Exception {
		when(webPlugin.isCollectPageLoadTimesPerRequest()).thenReturn(false);
		final MockHttpServletRequest req = new MockHttpServletRequest();
		final String requestName = "GET /test.html";
		req.addParameter("requestName", requestName);
		req.addParameter("serverTime", "100");
		req.addParameter("domProcessing", "10");
		req.addParameter("pageRendering", "30");
		req.addParameter("timeToFirstByte", "160");
		final MockHttpServletResponse resp = new MockHttpServletResponse();

		rumServlet.doGet(req, resp);

		assertEquals(200, resp.getStatus());

		assertNull(registry.getTimers().get(name("response_time").tag("request_name", requestName).tag("tier", "browser").type("dom_processing").build()));
		assertNull(registry.getTimers().get(name("response_time").tag("request_name", requestName).tag("tier", "browser").type("page_rendering").build()));
		assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "browser").type("dom_processing").build()));
		assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "browser").type("page_rendering").build()));
		assertEquals(TimeUnit.MILLISECONDS.toNanos(10), registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "browser").type("dom_processing").build()).getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(30), registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "browser").type("page_rendering").build()).getSnapshot().getMax());

		assertNull(registry.getTimers().get(name("response_time").tag("request_name", requestName).tag("tier", "network").build()));
		assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "network").build()));
		// t_resp-serverTime
		assertEquals(TimeUnit.MILLISECONDS.toNanos(60), registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "network").build()).getSnapshot().getMax());

		assertNull(registry.getTimers().get(name("response_time").tag("request_name", requestName).tag("tier", "total").build()));
		assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "total").build()));
		// t_page + t_resp
		assertEquals(TimeUnit.MILLISECONDS.toNanos(200), registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "total").build()).getSnapshot().getMax());

		assertNull(registry.getTimers().get(name("response_time").tag("request_name", requestName).tag("tier", "server_rum").build()));
		assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "server_rum").build()));
		assertEquals(TimeUnit.MILLISECONDS.toNanos(100), registry.getTimers().get(name("response_time").tag("request_name", "All").tag("tier", "server_rum").build()).getSnapshot().getMax());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMissingParam() throws Exception {
		final MockHttpServletRequest req = new MockHttpServletRequest();
		req.addParameter("requestName", "GET /test.html");
		req.addParameter("serverTime", "100");
		req.addParameter("domProcessing", "10");
		req.addParameter("pageRendering", "30");

		rumServlet.doGet(req, new MockHttpServletResponse());
	}

	@Test
	public void testRumDisabled() throws Exception {
		when(webPlugin.isRealUserMonitoringEnabled()).thenReturn(false);
		final MockHttpServletRequest req = new MockHttpServletRequest();
		final MockHttpServletResponse resp = new MockHttpServletResponse();
		rumServlet.doGet(req, resp);

		assertEquals(404, resp.getStatus());
	}
}
