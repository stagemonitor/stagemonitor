package org.stagemonitor.core.metrics.aspects;

import org.aspectj.lang.annotation.Pointcut;

public abstract class AbstractAspect {

	@Pointcut("execution(public * *(..))")
	public void publicMethod() {}

}
