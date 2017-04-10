package org.stagemonitor.core.metrics.annotations;

import com.codahale.metrics.annotation.Gauge;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.core.metrics.MonitorGauges;
import org.stagemonitor.core.metrics.aspects.SignatureUtils;

import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

/**
 * Implementation for the {@link Gauge} annotation
 */
public class GaugeTransformer extends StagemonitorByteBuddyTransformer {

	private static final Logger logger = LoggerFactory.getLogger(GaugeTransformer.class);

	@Override
	protected ElementMatcher.Junction<TypeDescription> getNarrowTypesMatcher() {
		return isAnnotatedWith(MonitorGauges.class);
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription> getMethodElementMatcher() {
		return isConstructor();
	}

	@Advice.OnMethodExit
	public static void gauges(@Advice.This Object thiz) {
		monitorGauges(thiz);
	}

	public static void monitorGauges(Object object) {
		for (final Method method : object.getClass().getDeclaredMethods()) {
			final Gauge gaugeAnnotation = method.getAnnotation(Gauge.class);
			// only create gauge, if method takes no parameters and is non-void
			if (gaugeAnnotation != null && methodTakesNoParamsAndIsNonVoid(method)) {
				method.setAccessible(true);
				final String signature = SignatureUtils.getSignature(object.getClass().getName(), method.getName(),
						gaugeAnnotation.name(), gaugeAnnotation.absolute());

				registerGauge(object, method, signature);
			}
		}
	}

	private static boolean methodTakesNoParamsAndIsNonVoid(Method method) {
		return method.getGenericParameterTypes().length == 0 && method.getReturnType() != Void.class;
	}

	private static void registerGauge(final Object object, final Method method, final String signature) {
		Stagemonitor.getMetric2Registry().registerNewMetrics(name("gauge_" + signature).build(), new com.codahale.metrics.Gauge() {
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
