package de.isys.jawap.benchmark;

import de.isys.jawap.collector.profiler.instrument.PerformanceMonitorAspect;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class TestPerformanceMonitorAspect extends PerformanceMonitorAspect {

	@Pointcut("execution(* de.isys.jawap.benchmark.ClassToProfile.*(..))")
	public void methodsToProfile() {
	}
}
