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
import org.stagemonitor.tracing.ExternalHttpRequest;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.profiler.Profiler;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class SpringRestTemplateContextPropagatingTransformer extends StagemonitorByteBuddyTransformer {

	@Override
	protected ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
		return named("org.springframework.http.client.support.InterceptingHttpAccessor");
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription> getMethodElementMatcher() {
		return isConstructor();
	}

	@Advice.OnMethodExit(inline = false)
	public static void onInterceptingHttpAccessorCreated(@Advice.This InterceptingHttpAccessor httpAccessor) {
		final TracingPlugin tracingPlugin = Stagemonitor.getPlugin(TracingPlugin.class);
		httpAccessor.getInterceptors().add(new SpringRestTemplateContextPropagatingInterceptor(tracingPlugin));
	}

	public static class SpringRestTemplateContextPropagatingInterceptor implements ClientHttpRequestInterceptor {

		private final TracingPlugin tracingPlugin;

		SpringRestTemplateContextPropagatingInterceptor(TracingPlugin tracingPlugin) {
			this.tracingPlugin = tracingPlugin;
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
			final Span span = new ExternalHttpRequest(tracingPlugin.getTracer(), request.getMethod().toString(), request.getURI().toString(), request.getURI().getHost(), request.getURI().getPort()).createSpan();
			try {
				Profiler.start(request.getMethod().toString() + " " + request.getURI() + " ");
				tracingPlugin.getTracer().inject(span.context(), Format.Builtin.HTTP_HEADERS, new SpringHttpRequestInjectAdapter(request));
				return execution.execute(request, body);
			} finally {
				Profiler.stop();
				span.finish();
			}
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
