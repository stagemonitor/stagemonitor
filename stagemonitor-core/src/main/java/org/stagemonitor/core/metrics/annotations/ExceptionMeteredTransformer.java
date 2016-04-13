package org.stagemonitor.core.metrics.annotations;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;

import com.codahale.metrics.annotation.ExceptionMetered;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;

public class ExceptionMeteredTransformer extends StagemonitorByteBuddyTransformer {

	@Override
	protected ElementMatcher.Junction<? super MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return isAnnotatedWith(ExceptionMetered.class);
	}

	@Advice.OnMethodExit
	public static void meterException(@ExceptionMeteredSignature String signature, @MeterExceptionsFor Class<? extends Exception> cause, @Advice.Thrown Throwable t) {
		if (t != null && cause.isInstance(t)) {
			Stagemonitor.getMetric2Registry().meter(name("exception_rate").tag("signature", signature).build()).mark();
		}
	}

	@Override
	protected List<StagemonitorDynamicValue<?>> getDynamicValues() {
		return Arrays.asList(new ExceptionMeteredTransformer.ExceptionMeteredSignatureDynamicValue(),
				new ExceptionMeteredTransformer.MeterExceptionsForDynamicValue());
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	protected @interface ExceptionMeteredSignature {
	}

	public static class ExceptionMeteredSignatureDynamicValue extends MetricAnnotationSignatureDynamicValue<ExceptionMeteredSignature> {

		protected NamingParameters getNamingParameters(MethodDescription.InDefinedShape instrumentedMethod) {
			final ExceptionMetered exceptionMetered = instrumentedMethod.getDeclaredAnnotations().ofType(ExceptionMetered.class).loadSilent();
			return new NamingParameters(exceptionMetered.name(), exceptionMetered.absolute());
		}

		@Override
		public Class<ExceptionMeteredSignature> getAnnotationClass() {
			return ExceptionMeteredSignature.class;
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface MeterExceptionsFor {
	}

	public static class MeterExceptionsForDynamicValue extends StagemonitorDynamicValue<MeterExceptionsFor> {
		@Override
		public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
							  ParameterDescription.InDefinedShape target,
							  AnnotationDescription.Loadable<MeterExceptionsFor> annotation, boolean initialized) {
			return instrumentedMethod.getDeclaredAnnotations().ofType(ExceptionMetered.class).loadSilent().cause();
		}

		@Override
		public Class<MeterExceptionsFor> getAnnotationClass() {
			return MeterExceptionsFor.class;
		}
	}
}
