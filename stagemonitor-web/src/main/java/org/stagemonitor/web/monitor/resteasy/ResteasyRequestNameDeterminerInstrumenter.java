package org.stagemonitor.web.monitor.resteasy;

import java.lang.reflect.Method;

import javassist.CtClass;
import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.requestmonitor.BusinessTransactionNamingStrategy;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

/**
 * A {@link StagemonitorJavassistInstrumenter} implementation for naming Resteasy requests.
 */
public class ResteasyRequestNameDeterminerInstrumenter extends StagemonitorJavassistInstrumenter {
	private static RequestMonitorPlugin requestMonitorPlugin = Stagemonitor.getConfiguration(RequestMonitorPlugin.class);

	@Override
	public boolean isIncluded(String className) {
		return className.equals("org/jboss/resteasy/core/ResourceMethodRegistry");
	}

	@Override
	public void transformClass(CtClass ctClass, ClassLoader loader) throws Exception {
		ctClass.getDeclaredMethod("getResourceInvoker")
				.insertAfter(
						"org.stagemonitor.web.monitor.resteasy.ResteasyRequestNameDeterminerInstrumenter"
								+ "setRequestNameByInvoker($_);");
	}

	/**
	 * Set the request name by looking up the Resteasy resource and method using the provided {@link ResourceInvoker}.
	 */
	public static void setRequestNameByInvoker(ResourceInvoker invoker) {
		if (RequestMonitor.getRequest() != null) {
			String requestName = getRequestNameFromInvoker(invoker,
						requestMonitorPlugin.getBusinessTransactionNamingStrategy());
			if (requestName != null) {
				RequestMonitor.getRequest().setName(requestName);
			}
		}
	}

	/**
	 * Gets the Resteasy request name using the given {@link org.jboss.resteasy.core.ResourceInvoker} to lookup the Resteasy resource class and
	 * method.
	 *
	 * <p>The naming strategy can be specified by the {@code businessTransactionNamingStrategy} parameter. Acceptable
	 * values can be found in {@link org.stagemonitor.requestmonitor.BusinessTransactionNamingStrategy}.
	 */
	private static String getRequestNameFromInvoker(ResourceInvoker invoker, BusinessTransactionNamingStrategy businessTransactionNamingStrategy) {
		if (invoker != null && invoker instanceof ResourceMethodInvoker) {
			Method resourceMethod = invoker.getMethod();
			return businessTransactionNamingStrategy.getBusinessTransationName(
					resourceMethod.getDeclaringClass().getSimpleName(), resourceMethod.getName());
		}
		return null;
	}

	static void setRequestMonitorPlugin(RequestMonitorPlugin requestMonitorPlugin) {
		ResteasyRequestNameDeterminerInstrumenter.requestMonitorPlugin = requestMonitorPlugin;
	}
}
