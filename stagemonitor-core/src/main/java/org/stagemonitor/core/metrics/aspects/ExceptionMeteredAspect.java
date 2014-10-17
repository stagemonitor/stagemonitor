package org.stagemonitor.core.metrics.aspects;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.ExceptionMetered;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.stagemonitor.core.Stagemonitor;

import static com.codahale.metrics.MetricRegistry.name;

@Aspect
public class ExceptionMeteredAspect extends AbstractAspect {

	private final MetricRegistry registry = Stagemonitor.getMetricRegistry();

	@AfterThrowing(value = "publicMethod() && execution(@com.codahale.metrics.annotation.ExceptionMetered * *(..)) && @annotation(meteredAnnotation)",
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
