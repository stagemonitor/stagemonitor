package org.stagemonitor.tracing.soap;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;

import java.util.ArrayList;
import java.util.List;

import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

/**
 * This class just makes sure that {@link Binding#setHandlerChain(List)} is invoked after calling {@link
 * javax.xml.ws.Service#getPort} {@link javax.xml.ws.Service#createDispatch}
 *
 * <p> This is important as {@link SoapHandlerTransformer} intercepts calls to {@link Binding#setHandlerChain(List)} and
 * adds the stagemonitor handlers </p>
 */
public class SoapClientTransformer extends StagemonitorByteBuddyTransformer {

	@Override
	public ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
		return named("javax.xml.ws.Service");
	}

	@Override
	protected boolean transformsCoreJavaClasses() {
		return true;
	}

	@Override
	protected ElementMatcher.Junction<ClassLoader> getClassLoaderMatcher() {
		return any();
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription> getExtraMethodElementMatcher() {
		return named("getPort").or(named("createDispatch").and(returns(named("javax.xml.ws.Dispatch"))));
	}

	@Advice.OnMethodExit
	private static void setHandlerChain(@Advice.Return Object portOrDispatch) {
		if (portOrDispatch instanceof BindingProvider) {
			final Binding binding = ((BindingProvider) portOrDispatch).getBinding();
			final List<Handler> handlerChain = binding.getHandlerChain();
			if (handlerChain != null) {
				binding.setHandlerChain(handlerChain);
			} else {
				binding.setHandlerChain(new ArrayList<Handler>());
			}
		}
	}

}
