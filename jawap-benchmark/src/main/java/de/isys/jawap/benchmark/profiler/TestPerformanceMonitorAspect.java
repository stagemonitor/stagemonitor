package de.isys.jawap.benchmark.profiler;

import de.isys.jawap.collector.profiler.ProfilingAspect;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class TestPerformanceMonitorAspect extends ProfilingAspect {

	@Pointcut("execution(* de.isys.jawap.benchmark.profiler.ClassToProfile.*(..))")
	public void methodsToProfile() {
	}
}
