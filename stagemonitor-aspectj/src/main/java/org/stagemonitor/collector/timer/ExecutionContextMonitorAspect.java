package org.stagemonitor.collector.timer;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.stagemonitor.collector.core.Configuration;
import org.stagemonitor.collector.core.StageMonitor;
import org.stagemonitor.collector.core.monitor.ExecutionContextMonitor;
import org.stagemonitor.collector.core.monitor.MonitoredMethodExecution;

@Aspect
public abstract class ExecutionContextMonitorAspect {

	protected Configuration configuration = StageMonitor.getConfiguration();
	protected ExecutionContextMonitor executionContextMonitor = new ExecutionContextMonitor(configuration);

	@Pointcut
	public abstract void methodsToMonitor();

	@Around("methodsToMonitor()")
	public Object monitorMethodCall(final ProceedingJoinPoint pjp) throws Exception {
		String className = pjp.getSignature().getDeclaringTypeName();
		className = className.substring(className.lastIndexOf('.') + 1, className.length());
		final String methodSignature = className + "#" + pjp.getSignature().getName();
		return executionContextMonitor.monitor(new MonitoredMethodExecution(methodSignature,
				new MonitoredMethodExecution.MethodExecution() {
					@Override
					public Object execute() throws Exception {
						try {
							return pjp.proceed();
						} catch (Throwable t) {
							return handleThrowable(t);
						}
					}
				})).getExecutionResult();
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
