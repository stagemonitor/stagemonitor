package org.stagemonitor.jdbc;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.requestmonitor.tracing.wrapper.StatelessSpanInterceptor;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.sql.Connection;

import javax.sql.DataSource;

import __redirected.org.stagemonitor.dispatcher.Dispatcher;

import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.stagemonitor.core.instrument.CachedClassLoaderMatcher.cached;
import static org.stagemonitor.core.instrument.CanLoadClassElementMatcher.canLoadClass;

/**
 * When the {@link DataSource} implementation is not loaded by the application {@link ClassLoader}, like it is common
 * in application servers like JBoss, the calls to stagemonitor can't be inserted directly but only reflectively.
 */
public class ReflectiveConnectionMonitoringTransformer extends ConnectionMonitoringTransformer {

	private static final Logger logger = LoggerFactory.getLogger(ReflectiveConnectionMonitoringTransformer.class);

	private static final String CONNECTION_MONITOR = ConnectionMonitor.class.getName();

	// [0]: ConnectionMonitor [1]: Method
	private static ThreadLocal<Object[]> connectionMonitorThreadLocal;

	public ReflectiveConnectionMonitoringTransformer() throws NoSuchMethodException {
		if (isActive()) {
			Dispatcher.getValues().putIfAbsent(CONNECTION_MONITOR, new ThreadLocal<Object[]>());
			connectionMonitorThreadLocal = Dispatcher.get(CONNECTION_MONITOR);
		}
	}

	/**
	 * If the ThreadLocal is added, the code added in {@link #addReflectiveMonitorMethodCall} gets active
	 * <p/>
	 * Using a ThreadLocal ensures that each application invokes its own instance of the ConnectionMonitor and that
	 * applications which are not monitored by stagemonitor are not influenced
	 */
	public static class ConnectionMonitorAddingSpanInterceptor extends StatelessSpanInterceptor {

		private final Method monitorGetConnectionMethod;

		public ConnectionMonitorAddingSpanInterceptor() throws NoSuchMethodException {
			monitorGetConnectionMethod = ConnectionMonitor.class
					.getMethod("monitorGetConnection", Connection.class, Object.class, long.class);
			makeReflectionInvocationFaster(monitorGetConnectionMethod);
		}

		@Override
		public void onStart(SpanWrapper spanWrapper) {
			connectionMonitorThreadLocal.set(new Object[]{connectionMonitor, monitorGetConnectionMethod});
		}

		@Override
		public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
			connectionMonitorThreadLocal.remove();
		}

	}

	private static void makeReflectionInvocationFaster(Method method) {
		try {
			method.setAccessible(true);
		} catch (SecurityException e) {
			// ignore
		}
	}

	/**
	 * Only applies if stagemonitor can't be loaded by this class loader. For example a module class loader which loaded
	 * the DataSource but does not have access to the application classes.
	 */
	@Override
	public ElementMatcher.Junction<ClassLoader> getClassLoaderMatcher() {
		return not(cached(canLoadClass("org.stagemonitor.core.Stagemonitor")));
	}

	@Advice.OnMethodEnter
	private static long addTimestampLocalVariable() {
		return System.nanoTime();
	}

	@Advice.OnMethodExit
	private static void addReflectiveMonitorMethodCall(@Advice.This Object dataSource, @Advice.Return(readOnly = false) Connection connection, @Advice.Enter long startTime) {
		try {
			Object[] connectionMonitor = (Object[]) ((ThreadLocal) Dispatcher.getValues().get("org.stagemonitor.jdbc.ConnectionMonitor")).get();
			if (connectionMonitor != null) {
				final Method connectionMonitorMethod = (Method) connectionMonitor[1];
				final long duration = System.nanoTime() - startTime;
				// In JBoss, this method is executed in the context of the module class loader which loads the DataSource
				// The connectionMonitor is not accessible from this class loader. That's why we have to use reflection.
				connection = (Connection) connectionMonitorMethod.invoke(connectionMonitor[0], connection, dataSource, duration);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Makes sure that no DataSources are instrumented twice, even if multiple stagemonitored applications are
	 * deployed on one application server
	 */
	@Override
	public AgentBuilder.RawMatcher getRawMatcher() {
		return new AvoidDuplicateTransformationsRawMatcher();
	}

	private static class AvoidDuplicateTransformationsRawMatcher implements AgentBuilder.RawMatcher {
		@Override
		public boolean matches(TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule, Class<?> classBeingRedefined, ProtectionDomain protectionDomain) {
			final String key = getClassAlreadyTransformedKey(typeDescription, classLoader);
			final boolean hasAlreadyBeenTransformed = Dispatcher.getValues().containsKey(key);
			if (DEBUG_INSTRUMENTATION) {
				logger.info("{}: {}", key, hasAlreadyBeenTransformed);
			}
			return !hasAlreadyBeenTransformed;
		}
	}

	private static String getClassAlreadyTransformedKey(TypeDescription typeDescription, ClassLoader classLoader) {
		return typeDescription.getName() + ClassUtils.getIdentityString(classLoader) + ".transformed";
	}

	@Override
	public void beforeTransformation(TypeDescription typeDescription, ClassLoader classLoader) {
		super.beforeTransformation(typeDescription, classLoader);
		final String key = getClassAlreadyTransformedKey(typeDescription, classLoader);
		if (DEBUG_INSTRUMENTATION) {
			logger.info("setting {}", key);
		}
		Dispatcher.getValues().put(key, Boolean.TRUE);
	}
}
