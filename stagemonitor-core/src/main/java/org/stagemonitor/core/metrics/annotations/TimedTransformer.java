package org.stagemonitor.core.metrics.annotations;

import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.core.metrics.metrics2.MetricName;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

/**
 * Implementation for the {@link Timed} annotation
 */
public class TimedTransformer extends StagemonitorByteBuddyTransformer {

	private final static MetricName.MetricNameTemplate metricNameTemplate = name("timer").templateFor("signature");

	@Override
	protected ElementMatcher.Junction<MethodDescription> getExtraMethodElementMatcher() {
		return isAnnotatedWith(Timed.class);
	}

	@Override
	protected List<Advice.OffsetMapping.Factory<? extends Annotation>> getOffsetMappingFactories() {
		return Collections.<Advice.OffsetMapping.Factory<?>>singletonList(new TimedSignatureDynamicValue());
	}

	@Advice.OnMethodEnter
	public static Timer.Context startTimer(@TimedSignature String signature) {
		return Stagemonitor.getMetric2Registry().timer(getTimerName(signature)).time();
	}

	public static MetricName getTimerName(String signature) {
		return metricNameTemplate.build(signature);
	}

	@Advice.OnMethodExit(onThrowable = Throwable.class)
	public static void stopTimer(@Advice.Enter Timer.Context timer) {
		timer.stop();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface TimedSignature {
	}

	public static class TimedSignatureDynamicValue extends MetricAnnotationSignatureDynamicValue<TimedSignature> {

		@Override
		protected NamingParameters getNamingParameters(MethodDescription instrumentedMethod) {
			if (instrumentedMethod.getDeclaredAnnotations().isAnnotationPresent(Timed.class)) {
				final Timed timed = instrumentedMethod.getDeclaredAnnotations().ofType(Timed.class).load();
				return new NamingParameters(timed.name(), timed.absolute());
			} else {
				return new NamingParameters("", false);
			}
		}

		@Override
		public Class<TimedSignature> getAnnotationType() {
			return TimedSignature.class;
		}
	}

}
