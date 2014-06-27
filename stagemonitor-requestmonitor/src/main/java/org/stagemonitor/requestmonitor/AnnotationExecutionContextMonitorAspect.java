package org.stagemonitor.requestmonitor;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class AnnotationExecutionContextMonitorAspect extends ExecutionContextMonitorAspect {

	@Pointcut("within(@org.stagemonitor.requestmonitor.MonitorRequests *)")
	public void annotatedWithMonitor() {}

	@Pointcut("execution(public * *(..))")
	public void publicMethod() {}

	@Pointcut("publicMethod() && annotatedWithMonitor()")
	public void publicMethodInsideAClassAnnotatedWithMonitored() {}

	@Pointcut("execution(@org.stagemonitor.requestmonitor.MonitorRequests * *(..))")
	public void methodAnnotatedWithMonitored() {}

	@Override
	@Pointcut("publicMethodInsideAClassAnnotatedWithMonitored() || methodAnnotatedWithMonitored()")
	public void methodsToMonitor() {
	}
}
