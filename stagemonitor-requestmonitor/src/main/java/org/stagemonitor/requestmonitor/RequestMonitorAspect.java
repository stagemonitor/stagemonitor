package org.stagemonitor.requestmonitor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.stagemonitor.core.StageMonitor;
import org.stagemonitor.core.configuration.Configuration;

@Aspect
public abstract class RequestMonitorAspect {

	protected Configuration configuration = StageMonitor.getConfiguration();
	protected RequestMonitor requestMonitor = new RequestMonitor(configuration);

	@Pointcut
	public abstract void methodsToMonitor();

	@Around("methodsToMonitor()")
	public Object monitorMethodCall(final ProceedingJoinPoint pjp) throws Exception {
		String className = pjp.getSignature().getDeclaringTypeName();
		className = className.substring(className.lastIndexOf('.') + 1, className.length());
		final String methodSignature = className + "#" + pjp.getSignature().getName();
		return requestMonitor.monitor(new MonitoredMethodRequest(methodSignature,
				new MonitoredMethodRequest.MethodExecution() {
					@Override
					public Object execute() throws Exception {
						try {
							return pjp.proceed();
						} catch (Throwable t) {
							return handleThrowable(t);
						}
					}
				}, pjp.getArgs())).getExecutionResult();
	}

	private Object handleThrowable(Throwable t) throws Exception {
		if (t instanceof Exception) {
			throw (Exception) t;
		}
		if (t instanceof Error) {
			throw (Error) t;
		}
		throw new RuntimeException(t);
	}

}
