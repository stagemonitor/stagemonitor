package org.stagemonitor.requestmonitor;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;

public class MonitorRequestsInstrumenter extends StagemonitorJavassistInstrumenter {

	private static final RequestMonitorPlugin configuration = Stagemonitor.getConfiguration(RequestMonitorPlugin.class);
	private static final RequestMonitor requestMonitor = configuration.getRequestMonitor();
	private static final Logger logger = LoggerFactory.getLogger(MonitorRequestsInstrumenter.class);

	@Override
	public void transformClass(CtClass ctClass, ClassLoader loader) throws Exception {
		for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
			MonitorRequests monitorRequests = (MonitorRequests) ctMethod.getAnnotation(MonitorRequests.class);
			if (monitorRequests != null) {
				monitorMethodCall(ctClass, ctMethod);
			}
		}
	}

	public static void monitorMethodCall(CtClass ctClass, CtMethod ctMethod) {
		final int modifiers = ctMethod.getModifiers();
		if (Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers) || Modifier.isInterface(modifiers) || ctClass != ctMethod.getDeclaringClass()) {
			return;
		}
		try {
			String signature = configuration.getBusinessTransactionNamingStrategy().getBusinessTransationName(ctMethod.getDeclaringClass().getSimpleName(), ctMethod.getName());
			ctMethod.insertBefore("org.stagemonitor.requestmonitor.MonitorRequestsInstrumenter.getRequestMonitor()" +
					".monitorStart(new org.stagemonitor.requestmonitor.MonitoredMethodRequest(\"" + signature + "\", null, $args));");

			ctMethod.addCatch("{" +
					"	org.stagemonitor.requestmonitor.MonitorRequestsInstrumenter.getRequestMonitor().recordException($e);" +
					"	throw $e;" +
					"}",
					ctClass.getClassPool().get(Exception.class.getName()), "$e");

			ctMethod.insertAfter("org.stagemonitor.requestmonitor.MonitorRequestsInstrumenter.getRequestMonitor().monitorStop();", true);
		} catch (CannotCompileException e) {
			logger.debug(e.getMessage(), e);
		} catch (NotFoundException e) {
			logger.debug(e.getMessage(), e);
		}
	}

	public static RequestMonitor getRequestMonitor() {
		return requestMonitor;
	}

}
