package org.stagemonitor.web.monitor.spring;

import javassist.CtClass;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.requestmonitor.BusinessTransactionNamingStrategy;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

public class SpringMvcRequestNameDeterminerInstrumenter extends StagemonitorJavassistInstrumenter {

	private static RequestMonitorPlugin requestMonitorPlugin = Stagemonitor.getPlugin(RequestMonitorPlugin.class);

	@Override
	public boolean isIncluded(String className) {
		return className.equals("org/springframework/web/servlet/DispatcherServlet");
	}

	@Override
	public void transformClass(CtClass ctClass, ClassLoader loader) throws Exception {
		ctClass.getDeclaredMethod("getHandler")
				.insertAfter("org.stagemonitor.web.monitor.spring.SpringMvcRequestNameDeterminerInstrumenter" +
						".setRequestNameByHandler($_);");
	}

	public static void setRequestNameByHandler(HandlerExecutionChain handler) {
		if (RequestMonitor.getRequest() != null) {
			final String requestNameFromHandler = getRequestNameFromHandler(handler, requestMonitorPlugin.getBusinessTransactionNamingStrategy());
			if (requestNameFromHandler != null) {
				RequestMonitor.getRequest().setName(requestNameFromHandler);
			}
		}
	}

	private static String getRequestNameFromHandler(HandlerExecutionChain handler, BusinessTransactionNamingStrategy businessTransactionNamingStrategy) {
		if (handler != null && handler.getHandler() instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler.getHandler();
			return businessTransactionNamingStrategy.getBusinessTransationName(handlerMethod.getBeanType().getSimpleName(),
					handlerMethod.getMethod().getName());
		}
		return null;
	}

}
