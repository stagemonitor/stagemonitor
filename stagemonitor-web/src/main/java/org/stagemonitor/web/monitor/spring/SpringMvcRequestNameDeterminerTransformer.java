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
import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.requestmonitor.BusinessTransactionNamingStrategy;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

public class SpringMvcRequestNameDeterminerTransformer extends StagemonitorByteBuddyTransformer {

	private static final String DISPATCHER_SERVLET_CLASS = "org.springframework.web.servlet.DispatcherServlet";

	@Override
	protected ElementMatcher<? super TypeDescription> getExtraIncludeTypeMatcher() {
		return named(DISPATCHER_SERVLET_CLASS);
	}

	@Override
	public boolean isActive() {
		return ClassUtils.isPresent(DISPATCHER_SERVLET_CLASS);
	}

	@Override
	protected ElementMatcher.Junction<? super MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return named("getHandler").and(returns(HandlerExecutionChain.class));
	}

	@Advice.OnMethodExit
	public static void afterGetHandler(@Advice.Return HandlerExecutionChain handler) {
		SpringMvcRequestNameDeterminerTransformer.setRequestNameByHandler(handler);
	}

	public static void setRequestNameByHandler(HandlerExecutionChain handler) {
		if (RequestMonitor.getRequest() != null) {
			final BusinessTransactionNamingStrategy namingStrategy = Stagemonitor.getPlugin(RequestMonitorPlugin.class)
					.getBusinessTransactionNamingStrategy();
			final String requestNameFromHandler = getRequestNameFromHandler(handler, namingStrategy);
			if (requestNameFromHandler != null) {
				RequestMonitor.getRequest().setName(requestNameFromHandler);
			}
		}
	}

	private static String getRequestNameFromHandler(HandlerExecutionChain handler, BusinessTransactionNamingStrategy businessTransactionNamingStrategy) {
		if (handler != null && handler.getHandler() instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler.getHandler();
			return businessTransactionNamingStrategy.getBusinessTransationName(handlerMethod.getBeanType().getSimpleName(),
					handlerMethod.getMethod().getName());
		}
		return null;
	}

}
