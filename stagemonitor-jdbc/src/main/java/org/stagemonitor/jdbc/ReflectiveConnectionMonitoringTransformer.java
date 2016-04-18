package org.stagemonitor.jdbc;

import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.stagemonitor.core.instrument.CachedClassLoaderMatcher.cached;
import static org.stagemonitor.core.instrument.CanLoadClassElementMatcher.canLoadClass;

import java.lang.reflect.Method;
import java.lang.stagemonitor.dispatcher.Dispatcher;
import java.sql.Connection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

/**
 * When the {@link DataSource} implementation is not loaded by the application {@link ClassLoader}, like it is common
 * in application servers like JBoss, the calls to stagemonitor can't be inserted directly but only reflectively.
 */
public class ReflectiveConnectionMonitoringTransformer extends ConnectionMonitoringTransformer {

	private static final String CONNECTION_MONITOR = ConnectionMonitor.class.getName();
	private static final String ALREADY_TRANSFORMED_KEY = "ReflectiveConnectionMonitoringTransformer.alreadyTransformed";

	private Set<String> alreadyTransformed;

	// [0]: ConnectionMonitor [1]: Method
	private static ThreadLocal<Object[]> connectionMonitorThreadLocal;

	public ReflectiveConnectionMonitoringTransformer() throws NoSuchMethodException {
		if (isActive()) {
			RequestMonitor requestMonitor = configuration.getConfig(RequestMonitorPlugin.class).getRequestMonitor();
			final Method monitorGetConnectionMethod = ConnectionMonitor.class
					.getMethod("monitorGetConnection", Connection.class, Object.class, long.class);
			makeReflectionInvocationFaster(monitorGetConnectionMethod);

			addConnectionMonitorToThreadLocalOnEachRequest(requestMonitor, monitorGetConnectionMethod);

			Dispatcher.getValues().putIfAbsent(CONNECTION_MONITOR, new ThreadLocal<Object[]>());
			connectionMonitorThreadLocal = Dispatcher.get(CONNECTION_MONITOR);

			Dispatcher.getValues().putIfAbsent(ALREADY_TRANSFORMED_KEY, Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>()));
			alreadyTransformed = Dispatcher.get(ALREADY_TRANSFORMED_KEY);
		}
	}

	/**
	 * If the ThreadLocal is added, the code added in {@link #addReflectiveMonitorMethodCall} gets active
	 * <p/>
	 * Using a ThreadLocal ensures that each application invokes its own instance of the ConnectionMonitor and that
	 * applications that are not monitored by stagemonitor are not influenced
	 */
	private void addConnectionMonitorToThreadLocalOnEachRequest(RequestMonitor requestMonitor, final Method monitorGetConnectionMethod) {
		requestMonitor.addOnBeforeRequestCallback(new Runnable() {
			public void run() {
				connectionMonitorThreadLocal.set(new Object[]{connectionMonitor, monitorGetConnectionMethod});
			}
		});
		// clean up
		requestMonitor.addOnAfterRequestCallback(new Runnable() {
			public void run() {
				connectionMonitorThreadLocal.remove();
			}
		});
	}

	private void makeReflectionInvocationFaster(Method method) {
		try {
			method.setAccessible(true);
		} catch (SecurityException e) {
			// ignore
		}
	}

	/**
	 * Makes sure that no DataSources are instrumented twice, even if multiple stagemonitored applications are
	 * deployed on one application server
	 */
	@Override
	public ElementMatcher.Junction<TypeDescription> getIncludeTypeMatcher() {
		return super.getIncludeTypeMatcher().and(new ElementMatcher<TypeDescription>() {
			@Override
			public boolean matches(TypeDescription target) {
				// actually already transformed should be a pair of the ClassLoader and the class name but
				// I can't get a reference to the classloader which wants to load the target type.
				// But this is probably negligible
				return !alreadyTransformed.contains(target.getName());
			}
		});
	}

	/**
	 * Only applies if stagemonitor can't be loaded by this class loader.
	 * For example a module class loader which loaded the DataSource but does not have access to the application classes.
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
				connection = (Connection) connectionMonitorMethod.invoke(connectionMonitor[0], connection, dataSource, duration);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader) {
		alreadyTransformed.add(typeDescription.getName());
	}
}
