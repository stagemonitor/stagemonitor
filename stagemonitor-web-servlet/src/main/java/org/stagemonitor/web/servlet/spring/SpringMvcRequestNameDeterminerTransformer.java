package org.stagemonitor.web.servlet.spring;

import com.uber.jaeger.context.TracingUtils;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.tracing.BusinessTransactionNamingStrategy;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.metrics.MetricsSpanEventListener;

import io.opentracing.Span;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

public class SpringMvcRequestNameDeterminerTransformer extends StagemonitorByteBuddyTransformer {

	@Override
	public ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
		return named("org.springframework.web.servlet.DispatcherServlet");
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription> getExtraMethodElementMatcher() {
		return named("getHandler").and(returns(named("org.springframework.web.servlet.HandlerExecutionChain")));
	}

	// method signatures mus not contain HandlerExecutionChain as this type is optional and introspecting the class
	// with reflection would fail then
	@Advice.OnMethodExit
	public static void afterGetHandler(@Advice.Return Object handler) {
		SpringMvcRequestNameDeterminerTransformer.setRequestNameByHandler(handler);
	}

	public static void setRequestNameByHandler(Object handler) {
		if (!TracingUtils.getTraceContext().isEmpty()) {
			final BusinessTransactionNamingStrategy namingStrategy = Stagemonitor.getPlugin(TracingPlugin.class)
					.getBusinessTransactionNamingStrategy();
			final String requestNameFromHandler = getRequestNameFromHandler(handler, namingStrategy);
			if (requestNameFromHandler != null) {
				final Span span = TracingPlugin.getCurrentSpan();
				span.setTag(MetricsSpanEventListener.ENABLE_TRACKING_METRICS_TAG, true);
				span.setOperationName(requestNameFromHandler);
			}
		}
	}

	private static String getRequestNameFromHandler(Object handler, BusinessTransactionNamingStrategy businessTransactionNamingStrategy) {
		final HandlerExecutionChain handlerExecutionChain = (HandlerExecutionChain) handler;
		if (handler != null && handlerExecutionChain.getHandler() instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handlerExecutionChain.getHandler();
			return businessTransactionNamingStrategy.getBusinessTransationName(handlerMethod.getBeanType().getSimpleName(),
					handlerMethod.getMethod().getName());
		}
		return null;
	}

}
