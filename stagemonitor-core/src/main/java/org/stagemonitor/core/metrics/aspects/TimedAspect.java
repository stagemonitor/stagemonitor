package org.stagemonitor.core.metrics.aspects;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.stagemonitor.core.Stagemonitor;

import static com.codahale.metrics.MetricRegistry.name;

@Aspect
public class TimedAspect extends AbstractAspect {

	private static final MetricRegistry registry = Stagemonitor.getMetricRegistry();

	@Around(value = "publicMethod() && execution(@com.codahale.metrics.annotation.Timed * *(..)) && @annotation(timedAnnotation)")
	public Object timed(ProceedingJoinPoint pjp, Timed timedAnnotation) throws Throwable {
		final String signature = SignatureUtils.getSignature(pjp.getSignature(), timedAnnotation.name(),
				timedAnnotation.absolute());
		return timeMethodCall(pjp, signature);
	}

	@Around(value = "publicMethod() && " +
			"(execution(@org.springframework.scheduling.annotation.Async * *(..)) " +
			"|| execution(@org.springframework.scheduling.annotation.Scheduled * *(..)) " +
			"|| execution(@org.springframework.scheduling.annotation.Schedules * *(..)) " +
			"|| execution(@javax.ejb.Asynchronous * *(..)) " +
			"|| execution(@javax.ejb.Schedule * *(..)) " +
			"|| execution(@javax.ejb.Schedules * *(..)))")
	public Object timeAsyncCall(ProceedingJoinPoint pjp) throws Throwable {
		final String signature = SignatureUtils.getSignature(pjp.getSignature(), "", false);
		return timeMethodCall(pjp, signature);
	}

	public static Object timeMethodCall(ProceedingJoinPoint pjp, String signature) throws Throwable {
		final Timer.Context time = registry.timer(name("timer", signature)).time();
		try {
			return pjp.proceed();
		} finally {
			time.stop();
		}
	}
}
