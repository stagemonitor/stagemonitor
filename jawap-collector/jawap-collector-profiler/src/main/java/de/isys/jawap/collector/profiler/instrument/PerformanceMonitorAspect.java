package de.isys.jawap.collector.profiler.instrument;

import de.isys.jawap.collector.profiler.Profiler;
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