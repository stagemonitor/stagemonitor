package org.stagemonitor.collector.timer;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class AnnotationExecutionContextMonitorAspect extends ExecutionContextMonitorAspect {

	@Pointcut("within(@org.stagemonitor.collector.timer.Monitored *)")
	public void annotatedWithMonitor() {}

	@Pointcut("execution(public * *(..))")
	public void publicMethod() {}

	@Pointcut("publicMethod() && annotatedWithMonitor()")
	public void publicMethodInsideAClassAnnotatedWithMonitored() {}

	@Pointcut("execution(@org.stagemonitor.collector.timer.Monitored * *(..))")
	public void methodAnnotatedWithMonitored() {}

	@Override
	@Pointcut("publicMethodInsideAClassAnnotatedWithMonitored() || methodAnnotatedWithMonitored()")
	public void methodsToMonitor() {
	}
}
