package org.stagemonitor.logging;

import com.codahale.metrics.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Creates {@link com.codahale.metrics.Timer}s for trace, debug, info, warn, error and fatal and updates them on every
 * log statement.
 */
@Aspect
public class TimeLoggingAspect extends AbstractLoggingAspect {

	@Around("loggingPointcut()")
	public Object timeLogging(ProceedingJoinPoint pjp) throws Throwable {
		final Timer.Context timer = registry.timer(name("logging", pjp.getSignature().getName())).time();
		try {
			return pjp.proceed();
		} finally {
			timer.stop();
		}
	}
}
