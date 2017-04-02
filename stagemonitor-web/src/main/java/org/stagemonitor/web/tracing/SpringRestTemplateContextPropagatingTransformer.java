package org.stagemonitor.web.tracing;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.InterceptingHttpAccessor;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.requestmonitor.AbstractExternalRequest;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.profiler.Profiler;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.tag.Tags;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class SpringRestTemplateContextPropagatingTransformer extends StagemonitorByteBuddyTransformer {

	@Override
	protected ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
		return named("org.springframework.http.client.support.InterceptingHttpAccessor");
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getMethodElementMatcher() {
		return isConstructor();
	}

	@Advice.OnMethodExit(inline = false)
	public static void onInterceptingHttpAccessorCreated(@Advice.This InterceptingHttpAccessor httpAccessor) {
		final RequestMonitorPlugin requestMonitorPlugin = Stagemonitor.getPlugin(RequestMonitorPlugin.class);
		httpAccessor.getInterceptors().add(new SpringRestTemplateContextPropagatingInterceptor(requestMonitorPlugin));
	}

	public static class SpringRestTemplateContextPropagatingInterceptor implements ClientHttpRequestInterceptor {

		private final RequestMonitorPlugin requestMonitorPlugin;

		private SpringRestTemplateContextPropagatingInterceptor(RequestMonitorPlugin requestMonitorPlugin) {
			this.requestMonitorPlugin = requestMonitorPlugin;
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
			final Span span = new ExternalHttpRequest(requestMonitorPlugin.getTracer(), request).createSpan();
			try {
				Profiler.start(request.getMethod().toString() + " " + request.getURI() + " ");
				requestMonitorPlugin.getTracer().inject(span.context(), Format.Builtin.HTTP_HEADERS, new SpringHttpRequestInjectAdapter(request));
				return execution.execute(request, body);
			} finally {
				Profiler.stop();
				span.finish();
			}
		}
	}

	public static class ExternalHttpRequest extends AbstractExternalRequest {

		private final HttpRequest request;

		protected ExternalHttpRequest(Tracer tracer, HttpRequest request) {
			super(tracer, request.getMethod().toString() + " " + request.getURI());
			this.request = request;
		}

		@Override
		public Span createSpan() {
			final Span span = super.createSpan();
			Tags.HTTP_URL.set(span, request.getURI().toString());
			Tags.PEER_HOSTNAME.set(span, request.getURI().getHost());
			Tags.PEER_PORT.set(span, (short) request.getURI().getPort());
			return span;
		}

		@Override
		protected String getType() {
			return "http";
		}
	}

	private static class SpringHttpRequestInjectAdapter implements TextMap {
		private final HttpRequest httpRequest;

		private SpringHttpRequestInjectAdapter(HttpRequest httpRequest) {
			this.httpRequest = httpRequest;
		}

		@Override
		public Iterator<Map.Entry<String, String>> iterator() {
			throw new UnsupportedOperationException("SpringHttpRequestInjectAdapter should only be used with Tracer.inject()");
		}

		@Override
		public void put(String key, String value) {
			httpRequest.getHeaders().add(key, value);
		}
	}
}
