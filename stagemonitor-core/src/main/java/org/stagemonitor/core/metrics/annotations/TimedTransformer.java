package org.stagemonitor.core.metrics.annotations;

import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

/**
 * Implementation for the {@link Timed} annotation
 */
public class TimedTransformer extends StagemonitorByteBuddyTransformer {

	private final static MetricName.MetricNameTemplate metricNameTemplate = name("timer").templateFor("signature");
	private final Set<Class<?>> asyncCallAnnotations = new HashSet<Class<?>>();

	public TimedTransformer() {
		asyncCallAnnotations.add(ClassUtils.forNameOrNull("org.springframework.scheduling.annotation.Async"));
		asyncCallAnnotations.add(ClassUtils.forNameOrNull("org.springframework.scheduling.annotation.Scheduled"));
		asyncCallAnnotations.add(ClassUtils.forNameOrNull("org.springframework.scheduling.annotation.Schedules"));
		asyncCallAnnotations.add(ClassUtils.forNameOrNull("javax.ejb.Asynchronous"));
		asyncCallAnnotations.add(ClassUtils.forNameOrNull("javax.ejb.Schedule"));
		asyncCallAnnotations.add(ClassUtils.forNameOrNull("javax.ejb.Schedules"));
		asyncCallAnnotations.remove(null);
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		ElementMatcher.Junction<MethodDescription.InDefinedShape> matcher = isAnnotatedWith(Timed.class);
		for (Class<?> annotation : asyncCallAnnotations) {
			matcher = matcher.or(isAnnotatedWith((Class<? extends Annotation>) annotation));
		}
		return matcher;
	}

	@Override
	protected List<StagemonitorDynamicValue<?>> getDynamicValues() {
		return Collections.<StagemonitorDynamicValue<?>>singletonList(new TimedSignatureDynamicValue());
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
				final Timed timed = instrumentedMethod.getDeclaredAnnotations().ofType(Timed.class).loadSilent();
				return new NamingParameters(timed.name(), timed.absolute());
			} else {
				return new NamingParameters("", false);
			}
		}

		@Override
		public Class<TimedSignature> getAnnotationClass() {
			return TimedSignature.class;
		}
	}

}
