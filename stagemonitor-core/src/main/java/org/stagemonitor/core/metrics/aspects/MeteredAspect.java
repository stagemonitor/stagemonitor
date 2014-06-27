package org.stagemonitor.core.metrics.aspects;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Metered;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.stagemonitor.core.StageMonitor;

import static com.codahale.metrics.MetricRegistry.name;

@Aspect
public class MeteredAspect {

	private final MetricRegistry registry = StageMonitor.getMetricRegistry();

	@Before(value = "execution(@com.codahale.metrics.annotation.Metered * *(..)) && @annotation(meteredAnnotation)")
	public void metered(JoinPoint.StaticPart jp, Metered meteredAnnotation) throws Throwable {
		final String signature = SignatureUtils.getSignature(jp.getSignature(), meteredAnnotation.name(),
				meteredAnnotation.absolute());
		registry.meter(name("meter", signature)).mark();
	}
}
