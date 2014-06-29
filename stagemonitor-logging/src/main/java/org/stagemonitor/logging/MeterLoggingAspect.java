package org.stagemonitor.logging;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Creates {@link com.codahale.metrics.Meter}s for trace, debug, info, warn, error and fatal and updates them on every
 * log statement.
 */
@Aspect
public class MeterLoggingAspect extends AbstractLoggingAspect {

	@Before("loggingPointcut()")
	public void beforeLogging(JoinPoint.StaticPart jp) {
		registry.meter(name("logging", jp.getSignature().getName())).mark();
	}
}
