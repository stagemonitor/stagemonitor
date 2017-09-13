package org.stagemonitor.core.metrics.annotations;

import com.codahale.metrics.annotation.ExceptionMetered;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.core.metrics.metrics2.MetricName;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class ExceptionMeteredTransformer extends StagemonitorByteBuddyTransformer {

	private static final MetricName.MetricNameTemplate metricNameTemplate = name("exception_rate").templateFor("signature");

	@Override
	protected ElementMatcher.Junction<MethodDescription> getExtraMethodElementMatcher() {
		return isAnnotatedWith(ExceptionMetered.class);
	}

	@Advice.OnMethodExit(onThrowable = Exception.class, inline = false)
	public static void meterException(@ExceptionMeteredSignature String signature, @MeterExceptionsFor Class<? extends Exception> cause, @Advice.Thrown Throwable e) {
		if (e != null && cause.isInstance(e)) {
			Stagemonitor.getMetric2Registry().meter(getMetricName(signature)).mark();
		}
	}

	public static MetricName getMetricName(String signature) {
		return metricNameTemplate.build(signature);
	}

	@Override
	protected List<Advice.OffsetMapping.Factory<? extends Annotation>> getOffsetMappingFactories() {
		return Arrays.asList(new ExceptionMeteredTransformer.ExceptionMeteredSignatureDynamicValue(),
				new ExceptionMeteredTransformer.MeterExceptionsForDynamicValue());
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	protected @interface ExceptionMeteredSignature {
	}

	public static class ExceptionMeteredSignatureDynamicValue extends MetricAnnotationSignatureDynamicValue<ExceptionMeteredSignature> {

		@Override
		protected NamingParameters getNamingParameters(MethodDescription instrumentedMethod) {
			final ExceptionMetered exceptionMetered = instrumentedMethod.getDeclaredAnnotations().ofType(ExceptionMetered.class).loadSilent();
			return new NamingParameters(exceptionMetered.name(), exceptionMetered.absolute());
		}

		@Override
		public Class<ExceptionMeteredSignature> getAnnotationType() {
			return ExceptionMeteredSignature.class;
		}

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface MeterExceptionsFor {
	}

	public static class MeterExceptionsForDynamicValue implements Advice.OffsetMapping.Factory<MeterExceptionsFor> {

		@Override
		public Advice.OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<MeterExceptionsFor> annotation, AdviceType adviceType) {
			return new Advice.OffsetMapping() {
				@Override
				public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context) {
					final ExceptionMetered exceptionMetered = instrumentedMethod.getDeclaredAnnotations().ofType(ExceptionMetered.class).loadSilent();
					final TypeDescription.ForLoadedType exceptionType = new TypeDescription.ForLoadedType(exceptionMetered.cause());
					return Target.ForStackManipulation.of(exceptionType);
				}
			};
		}

		@Override
		public Class<MeterExceptionsFor> getAnnotationType() {
			return MeterExceptionsFor.class;
		}
	}
}
