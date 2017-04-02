package org.stagemonitor.web.tracing;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.web.tracing.SpringRestTemplateContextPropagatingTransformer.SpringRestTemplateContextPropagatingInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SpringRestTemplateContextPropagatingTransformerTest {

	private RequestMonitorPlugin requestMonitorPlugin;
	private Server server;

	@Before
	public void setUp() throws Exception {
		Stagemonitor.init();
		Stagemonitor.startMonitoring(new MeasurementSession("SpringRestTemplateContextPropagatingTransformerTest", "test", "test"));
		requestMonitorPlugin = Stagemonitor.getPlugin(RequestMonitorPlugin.class);
		server = new Server(41234);

	}

	@After
	public void tearDown() throws Exception {
		server.stop();
	}

	@Test
	public void testRestTemplateHasInterceptor() throws Exception {
		final List<RestTemplate> restTemplates = Arrays.asList(
				new RestTemplate(),
				new RestTemplate(new SimpleClientHttpRequestFactory()),
				new RestTemplate(Collections.singletonList(new StringHttpMessageConverter())));
		for (RestTemplate restTemplate : restTemplates) {
			assertThat(restTemplate.getInterceptors().size(), is(1));
			assertThat(restTemplate.getInterceptors(), hasItem(isA(SpringRestTemplateContextPropagatingInterceptor.class)));
		}
	}

	@Test
	public void testB3HeaderContextPropagation() throws Exception {
		final AtomicBoolean handled = new AtomicBoolean(false);
		server.setHandler(new AbstractHandler() {
			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				baseRequest.setHandled(true);
				assertNotNull(request.getHeader("X-B3-TraceId"));
				handled.set(true);
			}
		});
		server.start();

		try (Span span = requestMonitorPlugin.getTracer()
				.buildSpan("testB3HeaderContextPropagation")
				.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).start()) {
			new RestTemplate().getForObject("http://localhost:41234", String.class);
			new RestTemplate().getForObject("http://localhost:41234", String.class);
		}
		assertTrue(handled.get());
	}
}
