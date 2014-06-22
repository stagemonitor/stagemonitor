package org.stagemonitor.collector.core.metrics.aspects;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.ExceptionMetered;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.stagemonitor.collector.core.StageMonitor;

import static com.codahale.metrics.MetricRegistry.name;

@Aspect
public class ExceptionMeteredAspect {

	private final MetricRegistry registry = StageMonitor.getMetricRegistry();

	@AfterThrowing(value = "execution(@com.codahale.metrics.annotation.ExceptionMetered * *(..)) && @annotation(meteredAnnotation)",
			throwing = "exception")
	public void metered(JoinPoint.StaticPart jp, ExceptionMetered meteredAnnotation, Exception exception) throws Throwable {
		final Class<? extends Throwable> cause = meteredAnnotation.cause();
		if ((cause != null && cause.isAssignableFrom(exception.getClass())) || cause == null) {
			final String signature = SignatureUtils.getSignature(jp.getSignature(), meteredAnnotation.name(),
					meteredAnnotation.absolute());
			registry.meter(name("meter", signature, ExceptionMetered.DEFAULT_NAME_SUFFIX)).mark();
		}
	}
}
