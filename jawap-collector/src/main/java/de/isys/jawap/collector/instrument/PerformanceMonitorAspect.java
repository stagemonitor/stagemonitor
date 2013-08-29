package de.isys.jawap.collector.instrument;

import de.isys.jawap.collector.model.MethodCallStats;
import de.isys.jawap.collector.profile.Profiler;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public abstract class PerformanceMonitorAspect {

	@Pointcut
	public abstract void methodsToProfile();

	@Pointcut("within(de.isys.jawap.collector..*)")
	public void jawapCollector() {
	}

	@Around("methodsToProfile() && !jawapCollector()")
	public Object profile(ProceedingJoinPoint pPjp) throws Throwable {
		System.out.println("> entering " + pPjp.getSignature().toShortString() + pPjp.getSourceLocation());
		MethodCallStats parent = Profiler.getMethodCallParent();
		if (parent != null) {
			MethodCallStats stats = Profiler.start(parent);
			try {
				return pPjp.proceed();
			} finally {
				System.out.println("< leaving  " + pPjp.getSignature().toShortString());

				Profiler.stop(stats, pPjp.getSignature().getDeclaringTypeName(), pPjp.getSignature().getName());
			}
		}
		return pPjp.proceed();
	}

}