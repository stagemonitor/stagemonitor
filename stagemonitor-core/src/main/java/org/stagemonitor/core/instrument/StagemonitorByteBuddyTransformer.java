package org.stagemonitor.core.instrument;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isFinal;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.isNative;
import static net.bytebuddy.matcher.ElementMatchers.isSubTypeOf;
import static net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static net.bytebuddy.matcher.ElementMatchers.isTypeInitializer;
import static net.bytebuddy.matcher.ElementMatchers.nameContains;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.not;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.ClassUtils;

public abstract class StagemonitorByteBuddyTransformer {

	public ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
		return new StagemonitorClassNameMatcher()
				.and(noInternalJavaClasses())
				.or(not(nameStartsWith("org.stagemonitor"))
						.and(noInternalJavaClasses())
						.and(getExtraIncludeTypeMatcher()))
				.and(not(isInterface()))
				.and(not(getExtraExcludeTypeMatcher()))
				.and(getExtraTypeMatcher())
				.and(not(isSubTypeOf(StagemonitorByteBuddyTransformer.class)))
				.and(not(isSubTypeOf(StagemonitorDynamicValue.class)));
	}

	public boolean isActive() {
		return true;
	}

	private ElementMatcher.Junction<NamedElement> noInternalJavaClasses() {
		return not(nameStartsWith("java").or(nameStartsWith("com.sun")));
	}

	protected ElementMatcher<? super TypeDescription> getExtraTypeMatcher() {
		return any();
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
				List<StagemonitorDynamicValue<?>> dynamicValues = getDynamicValues();

				Advice.WithCustomMapping withCustomMapping = Advice.withCustomMapping();
				for (StagemonitorDynamicValue dynamicValue : dynamicValues) {
					withCustomMapping = withCustomMapping.bind(dynamicValue.getAnnotationClass(), dynamicValue);
				}

				return builder
						.visit(withCustomMapping
								.to(getAdviceClass())
								.on(getMethodElementMatcher()));
			}

		};
	}

	protected List<StagemonitorDynamicValue<?>> getDynamicValues() {
		return Collections.emptyList();
	}


	protected ElementMatcher.Junction<? super MethodDescription.InDefinedShape> getMethodElementMatcher() {
		return not(isConstructor())
				.and(not(isAbstract()))
				.and(not(isNative()))
				.and(not(isFinal()))
				.and(not(isSynthetic()))
				.and(not(isTypeInitializer()))
				.and(not(nameContains("access$")))
				.and(getExtraMethodElementMatcher());
	}

	protected ElementMatcher.Junction<? super MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return any();
	}

	protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
		return getClass();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	protected @interface ProfilerSignature {
	}

	public abstract static class StagemonitorDynamicValue<T extends Annotation> implements Advice.DynamicValue<T> {
		public abstract Class<T> getAnnotationClass();
	}


}
