package org.stagemonitor.core.metrics.aspects;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.StageMonitor;

import java.lang.reflect.Method;

import static com.codahale.metrics.MetricRegistry.name;

@Aspect
public class GaugeAspect extends AbstractAspect {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final MetricRegistry registry = StageMonitor.getMetricRegistry();

	@Pointcut("execution((@org.stagemonitor.core.metrics.MonitorGauges *).new(..))")
	public void constructorCallsOfTypesAnnotatedWithMonitorGauges() {
	}

	@After(value = "constructorCallsOfTypesAnnotatedWithMonitorGauges() && this(object)", argNames = "pjp,object")
	public void gauge(final JoinPoint pjp, Object object) throws Throwable {
		String simpleClassName = pjp.getSignature().getDeclaringType().getSimpleName();
		for (final Method method : object.getClass().getDeclaredMethods()) {
			final com.codahale.metrics.annotation.Gauge gaugeAnnotation = method
					.getAnnotation(com.codahale.metrics.annotation.Gauge.class);
			// only create gauge, if method takes no parameters and is non-void
			if (gaugeAnnotation != null && methodTakesNoParamsAndIsNonVoid(method)) {
				method.setAccessible(true);
				final String signature = SignatureUtils.getSignature(simpleClassName, method.getName(),
						gaugeAnnotation.name(), gaugeAnnotation.absolute());

				registerGauge(object, method, signature);
			}
		}
	}

	private boolean methodTakesNoParamsAndIsNonVoid(Method method) {
		return method.getGenericParameterTypes().length == 0 && method.getReturnType() != Void.class;
	}

	private void registerGauge(final Object object, final Method method, final String signature) {
		registry.register(name("gauge", signature), new Gauge() {
			@Override
			public Object getValue() {
				try {
					return method.invoke(object);
				} catch (Exception e) {
					logger.warn("Error occurred while invoking gauge {}: {}", signature, e.getMessage());
					return null;
				}
			}
		});
	}
}
