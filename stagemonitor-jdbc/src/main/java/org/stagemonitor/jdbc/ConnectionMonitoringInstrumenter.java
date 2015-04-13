package org.stagemonitor.jdbc;

import java.io.IOException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.MainStagemonitorClassFileTransformer;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;

public class ConnectionMonitoringInstrumenter extends StagemonitorJavassistInstrumenter {

	private static final String CONNECTION_MONITOR = ConnectionMonitor.class.getName();
	private static final String MONITOR_GET_CONNECTION = CONNECTION_MONITOR + ".monitorGetConnection";
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final String STRING_CLASS_NAME = String.class.getName();
	private final Set<String> dataSourceImplementations;

	public static final ConnectionMonitor connectionMonitor = new ConnectionMonitor(Stagemonitor.getConfiguration(), Stagemonitor.getMetricRegistry());

	public ConnectionMonitoringInstrumenter() throws NoSuchMethodException {
		final Collection<String> impls = Stagemonitor.getConfiguration(JdbcPlugin.class).getDataSourceImplementations();
		dataSourceImplementations = new HashSet<String>();
		for (String impl : impls) {
			dataSourceImplementations.add(impl.replace('.', '/'));
		}
		final Method monitorGetConnectionMethod = connectionMonitor.getClass()
				.getMethod("monitorGetConnection", Connection.class, DataSource.class, long.class);

		System.getProperties().put(MONITOR_GET_CONNECTION, monitorGetConnectionMethod);
		System.getProperties().put(CONNECTION_MONITOR, connectionMonitor);
	}

	@Override
	public byte[] transformOtherClass(ClassLoader loader, String className, Class<?> classBeingRedefined,
									  ProtectionDomain protectionDomain, byte[] classfileBuffer)
			throws Exception {
		if (!dataSourceImplementations.contains(className)) {
			return classfileBuffer;
		}

		try {
			loader.loadClass("org.stagemonitor.core.Stagemonitor");
			return transformConnectionPool(loader, classfileBuffer, false);
		} catch (ClassNotFoundException e) {
			logger.info("The database connection pool {} is not loaded via the application class loader.", className);
			return transformConnectionPool(loader, classfileBuffer, true);
		}
	}

	private byte[] transformConnectionPool(ClassLoader loader, byte[] classfileBuffer, boolean invokeViaReflection)
			throws IOException, NotFoundException, CannotCompileException {

		final CtClass ctClass = MainStagemonitorClassFileTransformer.getCtClass(loader, classfileBuffer);
		for (CtMethod method : ctClass.getMethods()) {
			if (isGetConnectionMethod(method)) {
				instrument(ctClass, method, invokeViaReflection);
			}
		}
		return ctClass.toBytecode();
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
			throws CannotCompileException, NotFoundException {

		if (!ctClass.equals(method.getDeclaringClass())) {
			method = overrideMethod(ctClass, method);
		}

		method.addLocalVariable("$_stm_start", CtClass.longType);
		method.insertBefore("$_stm_start = System.nanoTime();");
		// $_ is the return value, which has to be casted to the return type ($r)
		if (!invokeViaReflection) {
			method.insertAfter("$_ = ($r) org.stagemonitor.jdbc.ConnectionMonitoringInstrumenter.connectionMonitor" +
					".monitorGetConnection($_, (javax.sql.DataSource) this, System.nanoTime() - $_stm_start);");
		} else {
			method.insertAfter("$_ = ($r) ((java.lang.reflect.Method) System.getProperties().get(\"" + MONITOR_GET_CONNECTION + "\"))" +
					".invoke(System.getProperties().get(\"" + CONNECTION_MONITOR + "\"), new Object[]{$_, (javax.sql.DataSource) this, Long.valueOf(System.nanoTime() - $_stm_start)});");
		}
	}

	private CtMethod overrideMethod(CtClass ctClass, CtMethod getConnectionMethodOfSuperclass)
			throws NotFoundException, CannotCompileException {
		final CtMethod m = CtNewMethod.delegator(getConnectionMethodOfSuperclass, ctClass);
		ctClass.addMethod(m);
		return m;
	}
}
