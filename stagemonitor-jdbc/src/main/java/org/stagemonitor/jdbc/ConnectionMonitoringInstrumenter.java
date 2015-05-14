package org.stagemonitor.jdbc;

import java.lang.instrument.ClassDefinition;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.stagemonitor.agent.StagemonitorAgent;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;

public class ConnectionMonitoringInstrumenter extends StagemonitorJavassistInstrumenter {

	private static final String CONNECTION_MONITOR = ConnectionMonitor.class.getName();
	private static final String MONITOR_GET_CONNECTION = CONNECTION_MONITOR + ".monitorGetConnection";

	private static final String STRING_CLASS_NAME = String.class.getName();
	private final Set<String> dataSourceImplementations = new HashSet<String>();

	public static ConnectionMonitor connectionMonitor;
	private final boolean active;

	public ConnectionMonitoringInstrumenter() throws NoSuchMethodException {
		this.active = ConnectionMonitor.isActive(Stagemonitor.getConfiguration(CorePlugin.class));
		if (active) {
			connectionMonitor = new ConnectionMonitor(Stagemonitor.getConfiguration(), Stagemonitor.getMetricRegistry());
			final Collection<String> impls = Stagemonitor.getConfiguration(JdbcPlugin.class).getDataSourceImplementations();
			for (String impl : impls) {
				dataSourceImplementations.add(impl.replace('.', '/'));
			}
			final Method monitorGetConnectionMethod = connectionMonitor.getClass()
					.getMethod("monitorGetConnection", Connection.class, DataSource.class, long.class);

			System.getProperties().put(MONITOR_GET_CONNECTION, monitorGetConnectionMethod);
			System.getProperties().put(CONNECTION_MONITOR, connectionMonitor);
		}
	}

	@Override
	public void transformClass(CtClass ctClass, ClassLoader loader) throws Exception {
		try {
			loader.loadClass("org.stagemonitor.core.Stagemonitor");
			transformConnectionPool(ctClass, false);
		} catch (ClassNotFoundException e) {
			// The database connection pool is not loaded via the application class loader
			// so ConnectionMonitor#monitorConnection has to be invoked via reflection
			transformConnectionPool(ctClass, true);
		}
	}

	@Override
	public boolean isIncluded(String className) {
		return active && dataSourceImplementations.contains(className);
	}

	private void transformConnectionPool(CtClass ctClass, boolean invokeViaReflection)
			throws Exception {

		for (CtMethod method : ctClass.getMethods()) {
			if (isGetConnectionMethod(method)) {
				instrument(ctClass, method, invokeViaReflection);
			}
		}
	}

	/**
	 * Checks if the method implements {@link javax.sql.DataSource#getConnection()} or
	 * {@link javax.sql.DataSource#getConnection(String, String)}
	 *
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

	private void instrument(CtClass ctClass, CtMethod method, boolean invokeViaReflection)
			throws Exception {

		final CtClass declaringClass = method.getDeclaringClass();
		if (declaringClass.isFrozen()) {
			declaringClass.defrost();
		}
		method.addLocalVariable("$_stm_start", CtClass.longType);
		method.insertBefore("$_stm_start = System.nanoTime();");
		// $_ is the return value, which has to be casted to the return type ($r)
		if (!invokeViaReflection) {
			method.insertAfter("$_ = ($r) org.stagemonitor.jdbc.ConnectionMonitoringInstrumenter.connectionMonitor" +
					".monitorGetConnection($_, (javax.sql.DataSource) this, System.nanoTime() - $_stm_start);");
		} else {
			final String methodParams = "new Object[]{$_, (javax.sql.DataSource) this, Long.valueOf(System.nanoTime() - $_stm_start)}";
			method.insertAfter("$_ = ($r) ((java.lang.reflect.Method) System.getProperties().get(\"" + MONITOR_GET_CONNECTION + "\"))" +
					".invoke(System.getProperties().get(\"" + CONNECTION_MONITOR + "\"), " + methodParams + ");");
		}

		if (!ctClass.equals(declaringClass)) {
			final ClassDefinition classDefinition = new ClassDefinition(Class.forName(declaringClass.getName()), declaringClass.toBytecode());
			StagemonitorAgent.getInstrumentation().redefineClasses(classDefinition);
		}
	}
}
