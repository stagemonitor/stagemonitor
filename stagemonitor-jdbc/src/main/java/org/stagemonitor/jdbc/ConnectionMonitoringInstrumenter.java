package org.stagemonitor.jdbc;

import java.lang.instrument.ClassDefinition;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.instrument.Dispatcher;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

public class ConnectionMonitoringInstrumenter extends StagemonitorJavassistInstrumenter {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final String CONNECTION_MONITOR = ConnectionMonitor.class.getName();

	private static final String STRING_CLASS_NAME = String.class.getName();

	// [0]: ConnectionMonitor [1]: Method
	private static ThreadLocal<Object[]> connectionMonitorThreadLocal;

	private final Set<String> dataSourceImplementations = new HashSet<String>();

	public static ConnectionMonitor connectionMonitor;

	private final boolean active;

	public ConnectionMonitoringInstrumenter() throws NoSuchMethodException {
		final Configuration configuration = Stagemonitor.getConfiguration();
		final Metric2Registry metric2Registry = Stagemonitor.getMetric2Registry();
		this.active = ConnectionMonitor.isActive(configuration.getConfig(CorePlugin.class));
		RequestMonitor requestMonitor = configuration.getConfig(RequestMonitorPlugin.class).getRequestMonitor();
		if (active) {
			connectionMonitor = new ConnectionMonitor(configuration, metric2Registry);
			final Method monitorGetConnectionMethod = connectionMonitor.getClass()
					.getMethod("monitorGetConnection", Connection.class, DataSource.class, long.class);
			makeReflectionInvocationFaster(monitorGetConnectionMethod);

			addConnectionMonitorToThreadLocalOnEachRequest(requestMonitor, monitorGetConnectionMethod);

			final Collection<String> impls = configuration.getConfig(JdbcPlugin.class).getDataSourceImplementations();
			for (String impl : impls) {
				dataSourceImplementations.add(impl.replace('.', '/'));
			}

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
	public synchronized void transformClass(CtClass ctClass, ClassLoader loader) throws Exception {
		// If the database connection pool is not loaded via the application class loader
		// ConnectionMonitor#monitorConnection has to be invoked via reflection
		boolean invokeViaReflection = !ClassUtils.canLoadClass(loader, "org.stagemonitor.core.Stagemonitor");
		instrument(ctClass, "getConnection", "()Ljava/sql/Connection;", invokeViaReflection);
		instrument(ctClass, "getConnection", "(Ljava/lang/String;Ljava/lang/String;)Ljava/sql/Connection;", invokeViaReflection);
	}

	@Override
	public boolean isIncluded(String className) {
		return active && dataSourceImplementations.contains(className);
	}

	/**
	 * Checks if the method implements {@link javax.sql.DataSource#getConnection()} or
	 * {@link javax.sql.DataSource#getConnection(String, String)}
	 */
	private boolean isGetConnectionMethod(CtMethod method) throws NotFoundException {
		if (method.getName().equals("getConnection")) {
			final CtClass[] parameterTypes = method.getParameterTypes();
			if (parameterTypes.length == 0) {
				return true;
			}
			if (parameterTypes.length == 2) {
				return parameterTypes[0].getName().equals(STRING_CLASS_NAME)
						&& parameterTypes[1].getName().equals(STRING_CLASS_NAME);
			}
		}
		return false;
	}

	private void instrument(CtClass ctClass, String name, String descriptor, boolean invokeViaReflection)
			throws Exception {
		CtMethod method;
		try {
			method = ctClass.getMethod(name, descriptor);
		} catch (NotFoundException e) {
			return;
		}

		if (Modifier.isAbstract(method.getModifiers())) {
			logger.warn("Trying to instrument a abstract method ({}) of class {}", method, ctClass.getName());
			return;
		}
		final CtClass declaringClass = method.getDeclaringClass();
		if (declaringClass.isFrozen()) {
			declaringClass.defrost();
		}
		method.addLocalVariable("$_stm_start", CtClass.longType);
		method.insertBefore("$_stm_start = System.nanoTime();");
		if (!invokeViaReflection) {
			addDirectMonitorMethodCall(method);
		} else {
			addReflectiveMonitorMethodCall(method);
		}

		if (!ctClass.equals(declaringClass)) {
			final ClassDefinition classDefinition = new ClassDefinition(Class.forName(declaringClass.getName()), declaringClass.toBytecode());
			ByteBuddyAgent.getInstrumentation().redefineClasses(classDefinition);
		}
	}

	private void addDirectMonitorMethodCall(CtMethod method) throws CannotCompileException {
		// $_ is the return value, which has to be casted to the return type ($r)
		method.insertAfter("$_ = ($r) org.stagemonitor.jdbc.ConnectionMonitoringInstrumenter.connectionMonitor" +
				".monitorGetConnection($_, (javax.sql.DataSource) this, System.nanoTime() - $_stm_start);");
	}

	private void addReflectiveMonitorMethodCall(CtMethod method) throws CannotCompileException {
		final String methodParams = "new Object[]{$_, (javax.sql.DataSource) this, Long.valueOf(System.nanoTime() - $_stm_start)}";
		method.insertAfter("{" +
				"	Object[] $_stm_connectionMonitor = (Object[])((ThreadLocal) org.stagemonitor.dispatcher.Dispatcher.getValues().get(\"" + CONNECTION_MONITOR + "\")).get();" +
				"	if ($_stm_connectionMonitor != null) {" +
				//		$_ is the return value, which has to be casted to the return type ($r)
				"		$_ = ($r) ((java.lang.reflect.Method) $_stm_connectionMonitor[1]).invoke($_stm_connectionMonitor[0], " + methodParams + ");" +
				"	}" +
				"}");
	}

	@Override
	public boolean isTransformClassesOfClassLoader(ClassLoader classLoader) {
		return true;
	}
}
