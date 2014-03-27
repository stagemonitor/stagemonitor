package org.stagemonitor.benchmark.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

@Aspect
public class AspectJAspect {

	public static long dummy = 0;


	@Around("execution(* org.stagemonitor.benchmark.aspectj.InstrumentationTestObject.around*(..))")
	public Object around(ProceedingJoinPoint pPjp) throws Throwable {
		dummy++;
		try {
			return pPjp.proceed();
		} finally {
			dummy++;
		}
	}

	@Pointcut("execution(* org.stagemonitor.benchmark.aspectj.InstrumentationTestObject.beforeAfter*(..))")
	public void beforeAfter() {
	}

	@Before("beforeAfter()")
	public void before() {
		dummy++;
	}

	@After("beforeAfter()")
	public void after() {
		dummy++;
	}
}
