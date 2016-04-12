package org.stagemonitor.core.instrument;

import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.not;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.codahale.metrics.annotation.ExceptionMetered;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.aspects.SignatureUtils;
import org.stagemonitor.core.util.ClassUtils;

public abstract class StagemonitorByteBuddyTransformer {

	public final ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
		return new StagemonitorClassNameMatcher()
				.and(noInternalJavaClasses())
				.or(not(nameStartsWith("org.stagemonitor"))
						.and(noInternalJavaClasses())
						.and(getExtraIncludeTypeMatcher()))
				.and(not(isInterface()))
				.and(not(getExtraExcludeTypeMatcher()));
	}

	private ElementMatcher.Junction<NamedElement> noInternalJavaClasses() {
		return not(nameStartsWith("java").or(nameStartsWith("com.sun")));
	}

	protected ElementMatcher<? super TypeDescription> getExtraIncludeTypeMatcher() {
		return none();
	}

	protected ElementMatcher<? super TypeDescription> getExtraExcludeTypeMatcher() {
		return none();
	}

	public ElementMatcher<ClassLoader> getClassLoaderMatcher() {
		return new ElementMatcher<ClassLoader>() {
			@Override
			public boolean matches(ClassLoader target) {
				return ClassUtils.canLoadClass(target, Stagemonitor.class.getName());
			}
		};
	}

	public AgentBuilder.Transformer getTransformer() {
		return new AgentBuilder.Transformer() {
			@Override
			public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
				return builder
						.visit(Advice.withCustomMapping()
								.bind(MetricsSignature.class, new SignatureDynamicValue())
								.bind(MeterExceptionsFor.class, new MeterExceptionsForDynamicValue())
								.to(getAdviceClass())
								.on(getMethodElementMatcher()));
			}

		};
	}

	protected ElementMatcher.Junction<? super MethodDescription.InDefinedShape> getMethodElementMatcher() {
		return none();
	}

	protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
		return getClass();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	protected @interface ProfilerSignature {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	protected @interface MetricsSignature {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	protected @interface MeterExceptionsFor {
	}

	private static class SignatureDynamicValue implements Advice.DynamicValue<MetricsSignature> {
		@Override
		public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
							  ParameterDescription.InDefinedShape target,
							  AnnotationDescription.Loadable<MetricsSignature> annotation,
							  boolean initialized) {

			String nameFromAnnotation = null;
			boolean absolute = false;
			final ExceptionMetered exceptionMetered = instrumentedMethod.getDeclaredAnnotations().ofType(ExceptionMetered.class).loadSilent();
			if (exceptionMetered != null) {
				nameFromAnnotation = exceptionMetered.name();
				absolute = exceptionMetered.absolute();
			}
			return SignatureUtils.getSignature(instrumentedMethod.getDeclaringType().getSimpleName(), instrumentedMethod.getName(), nameFromAnnotation, absolute);
		}
	}

	private static class MeterExceptionsForDynamicValue implements Advice.DynamicValue<MeterExceptionsFor> {
		@Override
		public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
							  ParameterDescription.InDefinedShape target,
							  AnnotationDescription.Loadable<MeterExceptionsFor> annotation, boolean initialized) {
			return instrumentedMethod.getDeclaredAnnotations().ofType(ExceptionMetered.class).loadSilent().cause();
		}
	}
}
