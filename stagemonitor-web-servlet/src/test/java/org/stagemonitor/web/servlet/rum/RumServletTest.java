package org.stagemonitor.web.servlet.rum;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.web.servlet.ServletPlugin;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class RumServletTest {

	private final Metric2Registry registry = new Metric2Registry();
	private ServletPlugin servletPlugin = mock(ServletPlugin.class);
	private RumServlet rumServlet = new RumServlet(registry, servletPlugin);

	@Before
	public void setUp() throws Exception {
		when(servletPlugin.isRealUserMonitoringEnabled()).thenReturn(true);
	}

	@Test
	public void testBeaconPerRequest() throws Exception {
		when(servletPlugin.isCollectPageLoadTimesPerRequest()).thenReturn(true);
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

		assertThat(registry.getTimers()).containsKey(name("response_time_rum").operationName(requestName).layer("Dom Processing").build());
		assertThat(registry.getTimers()).containsKey(name("response_time_rum").operationName(requestName).layer("Page Rendering").build());
		assertThat(registry.getTimers()).containsKey(name("response_time_rum").operationName("All").layer("Dom Processing").build());
		assertThat(registry.getTimers()).containsKey(name("response_time_rum").operationName("All").layer("Page Rendering").build());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(10), registry.getTimers().get(name("response_time_rum").operationName(requestName).layer("Dom Processing").build()).getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(30), registry.getTimers().get(name("response_time_rum").operationName(requestName).layer("Page Rendering").build()).getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(10), registry.getTimers().get(name("response_time_rum").operationName("All").layer("Dom Processing").build()).getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(30), registry.getTimers().get(name("response_time_rum").operationName("All").layer("Page Rendering").build()).getSnapshot().getMax());

		assertThat(registry.getTimers()).containsKey(name("response_time_rum").operationName(requestName).layer("Network").build());
		assertThat(registry.getTimers()).containsKey(name("response_time_rum").operationName("All").layer("Network").build());
		// t_resp-serverTime
		assertEquals(TimeUnit.MILLISECONDS.toNanos(60), registry.getTimers().get(name("response_time_rum").operationName(requestName).layer("Network").build()).getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(60), registry.getTimers().get(name("response_time_rum").operationName("All").layer("Network").build()).getSnapshot().getMax());

		assertThat(registry.getTimers()).containsKey(name("response_time_rum").operationName(requestName).layer("All").build());
		assertThat(registry.getTimers()).containsKey(name("response_time_rum").operationName("All").layer("All").build());
		// t_page + t_resp
		assertEquals(TimeUnit.MILLISECONDS.toNanos(200), registry.getTimers().get(name("response_time_rum").operationName(requestName).layer("All").build()).getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(200), registry.getTimers().get(name("response_time_rum").operationName("All").layer("All").build()).getSnapshot().getMax());

		assertThat(registry.getTimers()).containsKey(name("response_time_rum").operationName(requestName).layer("Server").build());
		assertThat(registry.getTimers()).containsKey(name("response_time_rum").operationName("All").layer("Server").build());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(100), registry.getTimers().get(name("response_time_rum").operationName(requestName).layer("Server").build()).getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(100), registry.getTimers().get(name("response_time_rum").operationName("All").layer("Server").build()).getSnapshot().getMax());
	}

	@Test
	public void testBeaconAggregate() throws Exception {
		when(servletPlugin.isCollectPageLoadTimesPerRequest()).thenReturn(false);
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

		assertThat(registry.getTimers()).doesNotContainKey(name("response_time_rum").operationName(requestName).layer("Dom Processing").build());
		assertThat(registry.getTimers()).doesNotContainKey(name("response_time_rum").operationName(requestName).layer("Page Rendering").build());
		assertThat(registry.getTimers()).containsKey(name("response_time_rum").operationName("All").layer("Dom Processing").build());
		assertThat(registry.getTimers()).containsKey(name("response_time_rum").operationName("All").layer("Page Rendering").build());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(10), registry.getTimers().get(name("response_time_rum").operationName("All").layer("Dom Processing").build()).getSnapshot().getMax());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(30), registry.getTimers().get(name("response_time_rum").operationName("All").layer("Page Rendering").build()).getSnapshot().getMax());

		assertThat(registry.getTimers()).doesNotContainKey(name("response_time_rum").operationName(requestName).layer("Network").build());
		assertThat(registry.getTimers()).containsKey(name("response_time_rum").operationName("All").layer("Network").build());
		// t_resp-serverTime
		assertEquals(TimeUnit.MILLISECONDS.toNanos(60), registry.getTimers().get(name("response_time_rum").operationName("All").layer("Network").build()).getSnapshot().getMax());

		assertThat(registry.getTimers()).doesNotContainKey(name("response_time_rum").operationName(requestName).layer("All").build());
		assertThat(registry.getTimers()).containsKey(name("response_time_rum").operationName("All").layer("All").build());
		// t_page + t_resp
		assertEquals(TimeUnit.MILLISECONDS.toNanos(200), registry.getTimers().get(name("response_time_rum").operationName("All").layer("All").build()).getSnapshot().getMax());

		assertThat(registry.getTimers()).doesNotContainKey(name("response_time_rum").operationName(requestName).layer("Server").build());
		assertThat(registry.getTimers()).containsKey(name("response_time_rum").operationName("All").layer("Server").build());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(100), registry.getTimers().get(name("response_time_rum").operationName("All").layer("Server").build()).getSnapshot().getMax());
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
		when(servletPlugin.isRealUserMonitoringEnabled()).thenReturn(false);
		final MockHttpServletRequest req = new MockHttpServletRequest();
		final MockHttpServletResponse resp = new MockHttpServletResponse();
		rumServlet.doGet(req, resp);

		assertEquals(404, resp.getStatus());
	}
}
