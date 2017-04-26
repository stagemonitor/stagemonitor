package org.stagemonitor.web.tracing;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.web.client.RestTemplate;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.tracing.B3HeaderFormat;
import org.stagemonitor.requestmonitor.tracing.B3Propagator;
import org.stagemonitor.web.tracing.SpringRestTemplateContextPropagatingTransformer.SpringRestTemplateContextPropagatingInterceptor;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.opentracing.mock.MockTracer;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpringRestTemplateContextPropagatingTransformerTest {

	private RequestMonitorPlugin requestMonitorPlugin;

	@Before
	public void setUp() throws Exception {
		requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		when(requestMonitorPlugin.getTracer()).thenReturn(new MockTracer(new B3Propagator()));
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
		HttpRequest httpRequest = new MockClientHttpRequest(HttpMethod.GET, new URI("http://example.com"));

		new SpringRestTemplateContextPropagatingInterceptor(requestMonitorPlugin)
				.intercept(httpRequest, null, mock(ClientHttpRequestExecution.class));

		assertTrue(httpRequest.getHeaders().containsKey(B3HeaderFormat.SPAN_ID_NAME));
		assertTrue(httpRequest.getHeaders().containsKey(B3HeaderFormat.TRACE_ID_NAME));
	}
}
