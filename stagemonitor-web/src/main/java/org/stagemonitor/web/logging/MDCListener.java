package org.stagemonitor.web.logging;

import java.util.UUID;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.MDC;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;

/**
 * This class adds the {@link MDC} properties requestId, application, host and instance.
 * <p/>
 * If you are using logback or log4j, you can append this to your pattern to append the properties to each log entry:
 * <code>R:[%X{requestId}] A:[%X{application}] H:[%X{host}] I:[%X{instance}]</code>
 */
@WebListener
public class MDCListener implements ServletRequestListener {

	public static final String STAGEMONITOR_REQUEST_ID_ATTR = "stagemonitor-request-id";

	@Override
	public void requestInitialized(ServletRequestEvent sre) {
		final MeasurementSession measurementSession = Stagemonitor.getMeasurementSession();
		MDC.put("application", measurementSession.getApplicationName());
		MDC.put("host", measurementSession.getHostName());
		String instanceName = measurementSession.getInstanceName();
		if (instanceName == null) {
			instanceName = sre.getServletRequest().getServerName();
		}
		MDC.put("instance", instanceName);

		final String requestId = UUID.randomUUID().toString();
		sre.getServletRequest().setAttribute(STAGEMONITOR_REQUEST_ID_ATTR, requestId);

		if (Stagemonitor.isStarted()) {
			// don't store the requestId in MDC if stagemonitor is not active
			// so that thread pools that get created on startup don't inherit the requestId
			MDC.put("requestId", requestId);
		}
	}

	@Override
	public void requestDestroyed(ServletRequestEvent sre) {
		// application, host and instance don't have to be removed, because they stay the same for all threads
		MDC.remove("requestId");
	}
}
