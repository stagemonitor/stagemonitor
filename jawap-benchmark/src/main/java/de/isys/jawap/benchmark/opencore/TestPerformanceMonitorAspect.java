package de.isys.jawap.benchmark.opencore;

import de.isys.jawap.collector.instrument.PerformanceMonitorAspect;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class TestPerformanceMonitorAspect extends PerformanceMonitorAspect {

	@Pointcut("execution(* de.isys.jawap.benchmark.opencore.OpenCoreBenchmark.*(..))")
	public void methodsToProfile() {
	}
}
