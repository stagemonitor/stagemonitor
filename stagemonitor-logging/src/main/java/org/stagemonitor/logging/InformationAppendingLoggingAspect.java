package org.stagemonitor.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestTrace;

/**
 * Appends the requestId, application name, instance name and host name to line to each logging statement. Example:
 * <pre>[requestId=8ecd9903-1f99-4d33-9582-23b0a9586bbf] [application=Spring PetClinic] [instance=localhost] [host=Felix-PC]</pre>
 *
 * @deprecated in favour of the {@link MDC} properties requestId, application, host and instance
 */
@Aspect
@Deprecated
public class InformationAppendingLoggingAspect extends AbstractLoggingAspect {

	@Around("loggingPointcut()")
	public Object appendInformation(ProceedingJoinPoint pjp) throws Throwable {
		RequestTrace request = RequestMonitor.getRequest();
		final String requestInformation = "[requestId=" + (request != null ? request.getId() : "") + "] " +
				Stagemonitor.getMeasurementSession().toString() + " ";
		final Object[] args = pjp.getArgs();
		if (args.length == 0) {
			return pjp.proceed();
		}
		if (args.length == 1 || args[0] instanceof String) {
			args[0] = requestInformation + args[0];
		} else if (args.length > 1 || args[1] instanceof String) {
			args[1] = requestInformation + args[1];
		}
		return pjp.proceed(args);
	}
}
