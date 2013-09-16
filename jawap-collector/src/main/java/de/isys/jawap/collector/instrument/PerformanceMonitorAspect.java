package de.isys.jawap.collector.instrument;

import de.isys.jawap.collector.profile.Profiler;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public abstract class PerformanceMonitorAspect {

	@Pointcut
	public abstract void methodsToProfile();

	@Pointcut("within(de.isys.jawap.collector..*)")
	private void jawapCollector() {
	}

	@Pointcut("methodsToProfile() && !jawapCollector()")
	private void applicationMethodsToProfile() {
	}

	@Before("applicationMethodsToProfile()")
	public void startProfiling() {
		Profiler.start();
	}

	@After("applicationMethodsToProfile()")
	public void stopProfiling(JoinPoint joinPoint) {
		Profiler.stop(joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().toLongString());
	}
}