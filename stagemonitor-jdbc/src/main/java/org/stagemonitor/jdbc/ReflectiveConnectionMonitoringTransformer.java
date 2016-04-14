package org.stagemonitor.jdbc;

import java.lang.reflect.Method;
import java.sql.Connection;

import javax.sql.DataSource;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.instrument.Dispatcher;
import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

/**
 * When the {@link DataSource} implementation is not loaded by the application {@link ClassLoader}, like it is common
 * in application servers like JBoss, the calls to stagemonitor can't be inserted directly but only reflectively.
 */
public class ReflectiveConnectionMonitoringTransformer extends ConnectionMonitoringTransformer {

	private static final Logger logger = LoggerFactory.getLogger(ReflectiveConnectionMonitoringTransformer.class);

	protected static final String CONNECTION_MONITOR = ConnectionMonitor.class.getName();

	// [0]: ConnectionMonitor [1]: Method
	private static ThreadLocal<Object[]> connectionMonitorThreadLocal;

	public ReflectiveConnectionMonitoringTransformer() throws NoSuchMethodException {
		super();
		if (isActive()) {
			RequestMonitor requestMonitor = configuration.getConfig(RequestMonitorPlugin.class).getRequestMonitor();
			connectionMonitor = new ConnectionMonitor(configuration, metric2Registry);
			final Method monitorGetConnectionMethod = ConnectionMonitor.class
					.getMethod("monitorGetConnection", Connection.class, DataSource.class, long.class);
			makeReflectionInvocationFaster(monitorGetConnectionMethod);

			addConnectionMonitorToThreadLocalOnEachRequest(requestMonitor, monitorGetConnectionMethod);

			connectionMonitorThreadLocal = Dispatcher.get(CONNECTION_MONITOR);
			if (connectionMonitorThreadLocal == null) {
				connectionMonitorThreadLocal = new ThreadLocal<Object[]>();
				Dispatcher.put(CONNECTION_MONITOR, connectionMonitorThreadLocal);
			}
		}
	}

	/**
	 * If the ThreadLocal is added, the code added in {@link #addReflectiveMonitorMethodCall} gets active
	 *
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

	@Override
	public ElementMatcher.Junction<ClassLoader> getClassLoaderMatcher() {
		return new ElementMatcher.Junction.AbstractBase<ClassLoader>() {
			@Override
			public boolean matches(ClassLoader target) {
				return !ClassUtils.canLoadClass(target, "org.stagemonitor.core.Stagemonitor") &&
						ClassUtils.canLoadClass(target, "org.stagemonitor.dispatcher.Dispatcher");
			}
		};
	}

	@Advice.OnMethodEnter
	private static long addTimestampLocalVariable() {
		return System.nanoTime();
	}

	@Advice.OnMethodExit
	private static void addReflectiveMonitorMethodCall(@Advice.This Object dataSource, @Advice.Return(readOnly = false) Connection connection, @Advice.Enter long startTime) {
		connection = monitorGetConnection(dataSource, connection, startTime);
	}

	public static Connection monitorGetConnection(Object dataSource, Connection connection, long startTime) {
		if (!(dataSource instanceof DataSource)) {
			return connection;
		}
		Object[] connectionMonitor = (Object[])((ThreadLocal) org.stagemonitor.dispatcher.Dispatcher.getValues().get(CONNECTION_MONITOR)).get();
		if (connectionMonitor != null) {
			try {
				final Method connectionMonitorMethod = (Method) connectionMonitor[1];
				final ConnectionMonitor connectionMonitorInstance = (ConnectionMonitor) connectionMonitor[0];
				final long duration = System.nanoTime() - startTime;
				return  (Connection) connectionMonitorMethod.invoke(connectionMonitorInstance, connection, dataSource, duration);
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
		return connection;
	}
}
