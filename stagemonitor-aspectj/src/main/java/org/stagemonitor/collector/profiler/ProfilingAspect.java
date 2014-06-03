package org.stagemonitor.collector.profiler;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public abstract class ProfilingAspect {

	@Pointcut
	public abstract void methodsToProfile();

	@Pointcut("within(org.stagemonitor.collector..*)")
	private void stagemonitorCollector() {
	}

	@Pointcut("!within(@org.aspectj.lang.annotation.Aspect)")
	private void dontWeaveInOtherAspects() {
	}

	@Pointcut("methodsToProfile() && !stagemonitorCollector() && dontWeaveInOtherAspects()")
	private void applicationMethodsToProfile() {
	}

	@Pointcut("if()")
	public static boolean isProfilingActive() {
		return Profiler.isProfilingActive();
	}

	@Before("applicationMethodsToProfile()")
	public void startProfiling() {
		Profiler.start();
	}

	@After("applicationMethodsToProfile()")
	public void stopProfiling(JoinPoint.StaticPart joinPoint) {
		Profiler.stop(joinPoint.getSignature().toString());
	}
}