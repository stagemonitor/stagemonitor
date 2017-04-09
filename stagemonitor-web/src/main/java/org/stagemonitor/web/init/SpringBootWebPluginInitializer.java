package org.stagemonitor.web.init;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.core.util.ClassUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public class SpringBootWebPluginInitializer extends StagemonitorByteBuddyTransformer {

	@Override
	protected ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
		return named("org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor");
	}

	@Override
	public boolean isActive() {
		return ClassUtils.isPresent("org.springframework.boot.web.servlet.ServletContextInitializer");
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getMethodElementMatcher() {
		return named("postProcessBeforeInitialization")
				.and(returns(Object.class))
				.and(takesArguments(Object.class, String.class));
	}

	@Advice.OnMethodExit(inline = false)
	public static void addInitializer(@Advice.Argument(0) Object bean) {
		if (bean instanceof ConfigurableEmbeddedServletContainer) {
			((ConfigurableEmbeddedServletContainer) bean).addInitializers(new StagemonitorServletContextInitializer());
		}
	}

	static class StagemonitorServletContextInitializer implements ServletContextInitializer {
		@Override
		public void onStartup(ServletContext servletContext) throws ServletException {
			ServletContainerInitializerUtil.registerStagemonitorServletContainerInitializers(servletContext);
		}
	}
}
