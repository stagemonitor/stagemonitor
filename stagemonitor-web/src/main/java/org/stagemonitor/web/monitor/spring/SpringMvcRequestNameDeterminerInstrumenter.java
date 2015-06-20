package org.stagemonitor.web.monitor.spring;

import javassist.CtClass;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.web.WebPlugin;

public class SpringMvcRequestNameDeterminerInstrumenter extends StagemonitorJavassistInstrumenter {

	private static WebPlugin webPlugin = Stagemonitor.getConfiguration(WebPlugin.class);
	private static RequestMonitorPlugin requestMonitorPlugin = Stagemonitor.getConfiguration(RequestMonitorPlugin.class);

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
			String requestName = "";
			if (handler != null) {
				requestName = SpringMonitoredHttpRequest.getRequestNameFromHandler(handler,
						requestMonitorPlugin.getBusinessTransactionNamingStrategy());
			}
			// requests with empty names don't get monitored
			// requestNames with non null values don't get overwritten and avoid GetNameCallback#getName to be called
			if (!requestName.isEmpty() || webPlugin.isMonitorOnlySpringMvcRequests()) {
				RequestMonitor.getRequest().setName(requestName);
			}
		}
	}

	public static void setWebPlugin(WebPlugin webPlugin) {
		SpringMvcRequestNameDeterminerInstrumenter.webPlugin = webPlugin;
	}
}
