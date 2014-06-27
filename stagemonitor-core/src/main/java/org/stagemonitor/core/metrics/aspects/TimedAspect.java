package org.stagemonitor.core.metrics.aspects;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.stagemonitor.core.StageMonitor;

import static com.codahale.metrics.MetricRegistry.name;

@Aspect
public class TimedAspect {

	private final MetricRegistry registry = StageMonitor.getMetricRegistry();

	@Around(value = "execution(@com.codahale.metrics.annotation.Timed * *(..)) && @annotation(timedAnnotation)")
	public Object timed(ProceedingJoinPoint pjp, Timed timedAnnotation) throws Throwable {
		final String signature = SignatureUtils.getSignature(pjp.getSignature(), timedAnnotation.name(),
				timedAnnotation.absolute());
		final Timer.Context time = registry.timer(name("timer", signature)).time();
		try {
			return pjp.proceed();
		} finally {
			time.stop();
		}
	}
}
