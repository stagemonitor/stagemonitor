package org.stagemonitor.web.rum;

import com.codahale.metrics.MetricRegistry;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.rum.RumServlet;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RumServletTest {

	private final MetricRegistry registry = new MetricRegistry();
	private WebPlugin webPlugin = mock(WebPlugin.class);
	private RumServlet rumServlet = new RumServlet(registry, webPlugin);

	@Test
	public void testBeaconPerRequest() throws Exception {
		when(webPlugin.isCollectPageLoadTimesPerRequest()).thenReturn(true);
		final MockHttpServletRequest req = new MockHttpServletRequest();
		req.addParameter("requestName", "GET /test.html");
		req.addParameter("serverTime", "100");
		req.addParameter("domProcessing", "10");
		req.addParameter("pageRendering", "30");
		req.addParameter("timeToFirstByte", "160");
		final MockHttpServletResponse resp = new MockHttpServletResponse();

		rumServlet.doGet(req, resp);

		assertEquals(200, resp.getStatus());

		assertNotNull(registry.getTimers().get("request.GET-|test:html.browser.time.dom-processing"));
		assertNotNull(registry.getTimers().get("request.GET-|test:html.browser.time.page-rendering"));
		assertNotNull(registry.getTimers().get("request.All.browser.time.dom-processing"));
		assertNotNull(registry.getTimers().get("request.All.browser.time.page-rendering"));
		assertEquals(TimeUnit.MILLISECONDS.toNanos(10), registry.getTimers().get("request.GET-|test:html.browser.time.dom-processing").getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(30), registry.getTimers().get("request.GET-|test:html.browser.time.page-rendering").getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(10), registry.getTimers().get("request.All.browser.time.dom-processing").getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(30), registry.getTimers().get("request.All.browser.time.page-rendering").getSnapshot().getMax());

		assertNotNull(registry.getTimers().get("request.GET-|test:html.network.time.total"));
		assertNotNull(registry.getTimers().get("request.All.network.time.total"));
		// t_resp-serverTime
		assertEquals(TimeUnit.MILLISECONDS.toNanos(60), registry.getTimers().get("request.GET-|test:html.network.time.total").getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(60), registry.getTimers().get("request.All.network.time.total").getSnapshot().getMax());

		assertNotNull(registry.getTimers().get("request.GET-|test:html.total.time.total"));
		assertNotNull(registry.getTimers().get("request.All.total.time.total"));
		// t_page + t_resp
		assertEquals(TimeUnit.MILLISECONDS.toNanos(200), registry.getTimers().get("request.GET-|test:html.total.time.total").getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(200), registry.getTimers().get("request.All.total.time.total").getSnapshot().getMax());

		assertNotNull(registry.getTimers().get("request.GET-|test:html.server-rum.time.total"));
		assertNotNull(registry.getTimers().get("request.All.server-rum.time.total"));
		assertEquals(TimeUnit.MILLISECONDS.toNanos(100), registry.getTimers().get("request.GET-|test:html.server-rum.time.total").getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(100), registry.getTimers().get("request.All.server-rum.time.total").getSnapshot().getMax());
	}

	@Test
	public void testBeaconAggregate() throws Exception {
		when(webPlugin.isCollectPageLoadTimesPerRequest()).thenReturn(false);
		final MockHttpServletRequest req = new MockHttpServletRequest();
		req.addParameter("requestName", "GET /test.html");
		req.addParameter("serverTime", "100");
		req.addParameter("domProcessing", "10");
		req.addParameter("pageRendering", "30");
		req.addParameter("timeToFirstByte", "160");
		final MockHttpServletResponse resp = new MockHttpServletResponse();

		rumServlet.doGet(req, resp);

		assertEquals(200, resp.getStatus());

		assertNull(registry.getTimers().get("request.GET-|test:html.browser.time.dom-processing"));
		assertNull(registry.getTimers().get("request.GET-|test:html.browser.time.page-rendering"));
		assertNotNull(registry.getTimers().get("request.All.browser.time.dom-processing"));
		assertNotNull(registry.getTimers().get("request.All.browser.time.page-rendering"));
		assertEquals(TimeUnit.MILLISECONDS.toNanos(10), registry.getTimers().get("request.All.browser.time.dom-processing").getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(30), registry.getTimers().get("request.All.browser.time.page-rendering").getSnapshot().getMax());

		assertNull(registry.getTimers().get("request.GET-|test:html.network.time.total"));
		assertNotNull(registry.getTimers().get("request.All.network.time.total"));
		// t_resp-serverTime
		assertEquals(TimeUnit.MILLISECONDS.toNanos(60), registry.getTimers().get("request.All.network.time.total").getSnapshot().getMax());

		assertNull(registry.getTimers().get("request.GET-|test:html.total.time.total"));
		assertNotNull(registry.getTimers().get("request.All.total.time.total"));
		// t_page + t_resp
		assertEquals(TimeUnit.MILLISECONDS.toNanos(200), registry.getTimers().get("request.All.total.time.total").getSnapshot().getMax());

		assertNull(registry.getTimers().get("request.GET-|test:html.server-rum.time.total"));
		assertNotNull(registry.getTimers().get("request.All.server-rum.time.total"));
		assertEquals(TimeUnit.MILLISECONDS.toNanos(100), registry.getTimers().get("request.All.server-rum.time.total").getSnapshot().getMax());
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
}
