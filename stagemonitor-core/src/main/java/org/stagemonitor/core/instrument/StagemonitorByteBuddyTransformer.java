package org.stagemonitor.core.instrument;

import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.not;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.ClassUtils;

public abstract class StagemonitorByteBuddyTransformer {

	public final ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
		return not(nameStartsWith("java").or(nameStartsWith("com.sun")))
				.and(new StagemonitorClassNameMatcher())
				.or(not(nameStartsWith("org.stagemonitor")).and(getExtraIncludeTypeMatcher()))
				.and(not(isInterface()))
				.and(not(getExtraExcludeTypeMatcher()));
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

	public abstract AgentBuilder.Transformer getTransformer();

}
