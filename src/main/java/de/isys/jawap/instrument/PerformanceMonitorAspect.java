package de.isys.jawap.instrument;

import de.isys.jawap.model.MethodCallStats;
import de.isys.jawap.profile.Profiler;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class PerformanceMonitorAspect {

	@Around("")
	public Object profile(ProceedingJoinPoint pPjp) throws Throwable {
		MethodCallStats parent = Profiler.getMethodCallParent();
		if (parent != null) {
			MethodCallStats stats = Profiler.start(parent);
			try {
				return pPjp.proceed();
			} finally {
				String targetClass = pPjp.getTarget().getClass().getSimpleName();
				String methodName = pPjp.getSignature().getName();
				Profiler.stop(stats, targetClass, methodName);
			}
		}
		return pPjp.proceed();
	}

}