package org.stagemonitor.web.monitor.spring;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.requestmonitor.BusinessTransactionNamingStrategy;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

public class SpringMvcRequestNameDeterminerTransformer extends StagemonitorByteBuddyTransformer {

	@Override
	public ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
		return named("org.springframework.web.servlet.DispatcherServlet");
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return named("getHandler").and(returns(named("org.springframework.web.servlet.HandlerExecutionChain")));
	}

	// method signatures mus not contain HandlerExecutionChain as this type is optional and introspecting the class
	// with reflection would fail then
	@Advice.OnMethodExit
	public static void afterGetHandler(@Advice.BoxedReturn Object handler) {
		SpringMvcRequestNameDeterminerTransformer.setRequestNameByHandler(handler);
	}

	public static void setRequestNameByHandler(Object handler) {
		if (RequestMonitor.get().getRequestTrace() != null) {
			final BusinessTransactionNamingStrategy namingStrategy = Stagemonitor.getPlugin(RequestMonitorPlugin.class)
					.getBusinessTransactionNamingStrategy();
			final String requestNameFromHandler = getRequestNameFromHandler(handler, namingStrategy);
			if (requestNameFromHandler != null) {
				RequestMonitor.get().getRequestTrace().setName(requestNameFromHandler);
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
