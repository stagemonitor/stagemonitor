package org.stagemonitor.tracing.soap;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.ws.Binding;
import javax.xml.ws.handler.Handler;

import __redirected.org.stagemonitor.dispatcher.Dispatcher;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isSubTypeOf;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;

public class SoapHandlerTransformer extends StagemonitorByteBuddyTransformer {

	private static final Logger logger = LoggerFactory.getLogger(SoapHandlerTransformer.class);

	public SoapHandlerTransformer() {
		final String key = "org.stagemonitor.tracing.soap.SoapHandlerTransformer";
		Dispatcher.getValues().putIfAbsent(key, new CopyOnWriteArrayList<Handler>());
		// using a list as there can be multiple applications using stagemonitor deployed to a single application server
		// this makes sure they don't override each other
		final List<Handler<?>> handlers = Arrays.<Handler<?>>asList(new TracingServerSOAPHandler(), new TracingClientSOAPHandler());
		Dispatcher.<List<Handler>>get(key).addAll(handlers);
		logger.info("Adding SOAPHandlers " + handlers);
		// remove handler on shutdown to avoid class loader leaks
		Stagemonitor.getPlugin(CorePlugin.class).closeOnShutdown(new Closeable() {
			@Override
			public void close() throws IOException {
				logger.info("Removing SOAP handlers " + handlers);
				Dispatcher.<List<Handler>>get(key).removeAll(handlers);
			}
		});
	}

	@Override
	public ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
		return not(ElementMatchers.isInterface()).and(isSubTypeOf(Binding.class));
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
	protected ElementMatcher.Junction<MethodDescription> getMethodElementMatcher() {
		return named("setHandlerChain");
	}

	/**
	 * This code might be executed in the context of the bootstrap class loader. That's why we have to make sure we only
	 * call code which is visible. For example, we can't use slf4j or directly reference stagemonitor classes
	 */
	@Advice.OnMethodEnter
	private static void addHandlers(@Advice.Argument(value = 0, readOnly = false) List<Handler> handlerChain, @Advice.This Binding binding) {
		final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("org.stagemonitor.tracing.soap.SoapHandlerTransformer");
		final List<Handler<?>> stagemonitorHandlers = Dispatcher.get("org.stagemonitor.tracing.soap.SoapHandlerTransformer");

		if (stagemonitorHandlers != null) {
			logger.fine("Adding SOAPHandlers " + stagemonitorHandlers + " to handlerChain for Binding " + binding);
			if (handlerChain == null) {
				handlerChain = Collections.emptyList();
			}
			// creating a new list as we don't know if handlerChain is immutable or not
			handlerChain = new ArrayList<Handler>(handlerChain);
			for (Handler<?> stagemonitorHandler : stagemonitorHandlers) {
				if (!handlerChain.contains(stagemonitorHandler) &&
						// makes sure we only add the handler to the correct application
						Dispatcher.isVisibleToCurrentContextClassLoader(stagemonitorHandler)) {
					handlerChain.add(stagemonitorHandler);
				}
			}
			logger.fine("Handler Chain: " + handlerChain);
		} else {
			logger.fine("No SOAPHandlers found in Dispatcher for Binding " + binding);
		}
	}

}
