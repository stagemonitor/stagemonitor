package org.stagemonitor.requestmonitor;

import javassist.CtClass;
import javassist.CtMethod;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;

public class MonitorRequestsInstrumenter extends StagemonitorJavassistInstrumenter {

	private static final RequestMonitorPlugin configuration = Stagemonitor.getConfiguration(RequestMonitorPlugin.class);
	private static final RequestMonitor requestMonitor = configuration.getRequestMonitor();

	@Override
	public void transformClass(CtClass ctClass, ClassLoader loader) throws Exception {
		for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
			MonitorRequests monitorRequests = (MonitorRequests) ctMethod.getAnnotation(MonitorRequests.class);
			if (monitorRequests != null) {
				String signature = configuration.getBusinessTransactionNamingStrategy().getBusinessTransationName(ctMethod.getDeclaringClass().getSimpleName(), ctMethod.getName());
				ctMethod.insertBefore("org.stagemonitor.requestmonitor.MonitorRequestsInstrumenter.getRequestMonitor()" +
						".monitorStart(new org.stagemonitor.requestmonitor.MonitoredMethodRequest(\"" + signature + "\", null, $args));");

				ctMethod.addCatch("{" +
						"	org.stagemonitor.requestmonitor.MonitorRequestsInstrumenter.getRequestMonitor().recordException($e);" +
						"	throw $e;" +
						"}",
						ctClass.getClassPool().get(Exception.class.getName()), "$e");

				ctMethod.insertAfter("org.stagemonitor.requestmonitor.MonitorRequestsInstrumenter.getRequestMonitor().monitorStop();", true);
			}
		}
	}

	public static RequestMonitor getRequestMonitor() {
		return requestMonitor;
	}

}
