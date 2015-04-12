package org.stagemonitor.web.monitor.spring;

import static org.stagemonitor.core.instrument.MainStagemonitorClassFileTransformer.getCtClass;

import java.security.ProtectionDomain;

import javassist.CtClass;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.web.WebPlugin;

public class SpringMvcRequestNameDeterminerInstrumenter extends StagemonitorJavassistInstrumenter {

	private static WebPlugin webPlugin = Stagemonitor.getConfiguration(WebPlugin.class);

	@Override
	public byte[] transformOtherClass(ClassLoader loader, String className, Class<?> classBeingRedefined,
									  ProtectionDomain protectionDomain, byte[] classfileBuffer)
			throws Exception {
		if (!className.equals("org/springframework/web/servlet/DispatcherServlet")) {
			return classfileBuffer;
		}
		CtClass ctClass = getCtClass(loader, classfileBuffer);
		try {
			ctClass.getDeclaredMethod("getHandler")
					.insertAfter("org.stagemonitor.web.monitor.spring.SpringMvcRequestNameDeterminerInstrumenter" +
							".setRequestNameByHandler($_);");
			return ctClass.toBytecode();
		} finally {
			ctClass.detach();
		}
	}

	public static void setRequestNameByHandler(HandlerExecutionChain handler) {
		if (RequestMonitor.getRequest() != null) {
			String requestName = "";
			if (handler != null) {
				requestName = SpringMonitoredHttpRequest.getRequestNameFromHandler(handler);
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
