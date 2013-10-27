package de.isys.jawap.benchmark.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

@Aspect
public class AspectJAspect {

	public static int dummy = 0;


	@Around("execution(* de.isys.jawap.benchmark.aspectj.InstrumentationTestObject.instrumentationAroundTestMethod(..))")
	public Object around(ProceedingJoinPoint pPjp) throws Throwable {
		dummy++;
		try {
			return pPjp.proceed();
		} finally {
			dummy++;
		}
	}

	@Pointcut("execution(* de.isys.jawap.benchmark.aspectj.InstrumentationTestObject.instrumentationBeforeAfterTestMethod(..))")
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
