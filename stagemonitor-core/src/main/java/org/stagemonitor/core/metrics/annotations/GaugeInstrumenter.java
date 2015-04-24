package org.stagemonitor.core.metrics.annotations;

import static com.codahale.metrics.MetricRegistry.name;

import java.lang.reflect.Method;

import com.codahale.metrics.annotation.Gauge;
import javassist.CtClass;
import javassist.CtConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.core.metrics.MonitorGauges;
import org.stagemonitor.core.metrics.aspects.SignatureUtils;

/**
 * Implementation for the {@link Gauge} annotation
 */
public class GaugeInstrumenter extends StagemonitorJavassistInstrumenter {

	private static final Logger logger = LoggerFactory.getLogger(GaugeInstrumenter.class);

	@Override
	public void transformClass(CtClass ctClass, ClassLoader loader) throws Exception {
		final MonitorGauges gaugeAnnotation = (MonitorGauges) ctClass.getAnnotation(MonitorGauges.class);
		if (gaugeAnnotation != null) {
			for (CtConstructor ctConstructor : ctClass.getDeclaredConstructors()) {
				// a constructor either calls this() super()
				// if it does not explicitly call super() it is called implicitly
				// by checking callsSuper(), we make sure that we're not instrumenting the class twice
				if (ctConstructor.isConstructor() && ctConstructor.callsSuper()) {
					ctConstructor.insertAfter("org.stagemonitor.core.metrics.annotations.GaugeInstrumenter.monitorGauges(this);");
				}
			}
		}
	}

	public static void monitorGauges(Object object) {
		String simpleClassName = object.getClass().getSimpleName();
		for (final Method method : object.getClass().getDeclaredMethods()) {
			final Gauge gaugeAnnotation = method.getAnnotation(Gauge.class);
			// only create gauge, if method takes no parameters and is non-void
			if (gaugeAnnotation != null && methodTakesNoParamsAndIsNonVoid(method)) {
				method.setAccessible(true);
				final String signature = SignatureUtils.getSignature(simpleClassName, method.getName(),
						gaugeAnnotation.name(), gaugeAnnotation.absolute());

				registerGauge(object, method, signature);
			}
		}
	}

	private static boolean methodTakesNoParamsAndIsNonVoid(Method method) {
		return method.getGenericParameterTypes().length == 0 && method.getReturnType() != Void.class;
	}

	private static void registerGauge(final Object object, final Method method, final String signature) {
		Stagemonitor.getMetricRegistry().register(name("gauge", signature), new com.codahale.metrics.Gauge() {
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
