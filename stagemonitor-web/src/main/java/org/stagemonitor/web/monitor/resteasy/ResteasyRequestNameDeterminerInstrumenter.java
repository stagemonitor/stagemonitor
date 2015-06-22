package org.stagemonitor.web.monitor.resteasy;

import javassist.CtClass;
import org.jboss.resteasy.core.ResourceInvoker;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.web.WebPlugin;

/**
 * A {@link StagemonitorJavassistInstrumenter} implementation for naming Resteasy requests.
 */
public class ResteasyRequestNameDeterminerInstrumenter extends StagemonitorJavassistInstrumenter {
	private static WebPlugin webPlugin = Stagemonitor.getConfiguration(WebPlugin.class);
	private static RequestMonitorPlugin requestMonitorPlugin = Stagemonitor.getConfiguration(RequestMonitorPlugin.class);

	@Override
	public boolean isIncluded(String className) {
		return className.equals("org/jboss/resteasy/core/ResourceMethodRegistry");
	}

	@Override
	public void transformClass(CtClass ctClass, ClassLoader loader) throws Exception {
		ctClass.getDeclaredMethod("getResourceInvoker")
				.insertAfter(
						"com.cerner.beadledom.stagemonitor.request.ResteasyRequestNameDeterminerInstrumenter"
								+ "setRequestNameByInvoker($_);");
	}

	/**
	 * Set the request name by looking up the Resteasy resource and method using the provided {@link ResourceInvoker}.
	 */
	public static void setRequestNameByInvoker(ResourceInvoker invoker) {
		if (RequestMonitor.getRequest() != null) {
			String requestName = "";
			if (invoker != null) {
				requestName = ResteasyMonitoredHttpRequest.getRequestNameFromInvoker(invoker,
						requestMonitorPlugin.getBusinessTransactionNamingStrategy());
			}
			// requests with empty names don't get monitored
			// requestNames with non null values don't get overwritten and avoid GetNameCallback#getName to be called
			if (!requestName.isEmpty() || webPlugin.isMonitorOnlyResteasyRequests()) {
				RequestMonitor.getRequest().setName(requestName);
			}
		}
	}

	static void setWebPlugin(WebPlugin webPlugin) {
		ResteasyRequestNameDeterminerInstrumenter.webPlugin = webPlugin;
	}

	static void setRequestMonitorPlugin(RequestMonitorPlugin requestMonitorPlugin) {
		ResteasyRequestNameDeterminerInstrumenter.requestMonitorPlugin = requestMonitorPlugin;
	}
}
