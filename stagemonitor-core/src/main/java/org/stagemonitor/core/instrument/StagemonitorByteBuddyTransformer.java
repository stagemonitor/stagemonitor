package org.stagemonitor.core.instrument;

import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.not;

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
						.visit(Advice.to(getAdviceClass()).on(getMethodElementMatcher()));
			}

		};
	}

	protected ElementMatcher.Junction<? super MethodDescription.InDefinedShape> getMethodElementMatcher() {
		return none();
	}

	protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
		return getClass();
	}

}
