package org.stagemonitor.tracing.soap;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.core.util.ClassUtils;

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
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.isSubTypeOf;
import static net.bytebuddy.matcher.ElementMatchers.nameContains;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;

public class SoapHandlerTransformer extends StagemonitorByteBuddyTransformer {

	public static final String JAVAX_XML_WS_HANDLER_HANDLER = "javax.xml.ws.handler.Handler";
	private static final Logger logger = LoggerFactory.getLogger(SoapHandlerTransformer.class);
	private static final String STAGEMONITOR_TRACING_SOAP_SOAP_HANDLER_TRANSFORMER = "org.stagemonitor.tracing.soap.SoapHandlerTransformer";

	public SoapHandlerTransformer() {
		if (isActive()) {
			final String key = STAGEMONITOR_TRACING_SOAP_SOAP_HANDLER_TRANSFORMER;
			Dispatcher.getValues().putIfAbsent(key, new CopyOnWriteArrayList<Handler>());
			// using a list as there can be multiple applications using stagemonitor deployed to a single application server
			// this makes sure they don't override each other
			final List<Handler<?>> handlers = Arrays.<Handler<?>>asList(new TracingServerSOAPHandler(), new TracingClientSOAPHandler());
			Dispatcher.<List<Handler>>get(key).addAll(handlers);
			logger.info("Adding SOAPHandlers {}", handlers);
			// remove handler on shutdown to avoid class loader leaks
			Stagemonitor.getPlugin(CorePlugin.class).closeOnShutdown(new Closeable() {
				@Override
				public void close() throws IOException {
					logger.info("Removing SOAP handlers {}", handlers);
					Dispatcher.<List<Handler>>get(key).removeAll(handlers);
				}
			});
		} else {
			logger.warn("{} is disabled because of missing com.sun.xml.ws:jaxws-rt. If you running Java >= 11 and you want "
					  + "monitor SOAP Calls then make sure com.sun.xml.ws:jaxws-rt is in classpath", SoapTracingPlugin.class.getSimpleName());
		}
	}

	/**
	 * This code might be executed in the context of the bootstrap class loader. That's why we have to make sure we only
	 * call code which is visible. For example, we can't use slf4j or directly reference stagemonitor classes
	 */
	@Advice.OnMethodEnter
	private static void addHandlers(@Advice.Argument(value = 0, readOnly = false) List<Handler> handlerChain, @Advice.This Binding binding) {
		final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(STAGEMONITOR_TRACING_SOAP_SOAP_HANDLER_TRANSFORMER);
		final List<Handler<?>> stagemonitorHandlers = Dispatcher.get(STAGEMONITOR_TRACING_SOAP_SOAP_HANDLER_TRANSFORMER);

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

	@Override
	public ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
		// It's important to pre-select potential matches first with the nameContains matcher
		// otherwise, the type hierarchy of each and every class has to be determined whether it derives from Binding
		return nameContains("Binding")
			.and(not(isInterface()))
			.and(isSubTypeOf(Binding.class));
	}

	@Override
	protected boolean transformsCoreJavaClasses() {
		return true;
	}

	@Override
	public boolean isActive() {
		return ClassUtils.isPresent(JAVAX_XML_WS_HANDLER_HANDLER);
	}

	@Override
	protected ElementMatcher.Junction<ClassLoader> getClassLoaderMatcher() {
		return any();
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription> getMethodElementMatcher() {
		return named("setHandlerChain");
	}

}
