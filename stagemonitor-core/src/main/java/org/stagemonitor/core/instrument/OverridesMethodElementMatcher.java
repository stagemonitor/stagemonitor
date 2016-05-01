package org.stagemonitor.core.instrument;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.declaresMethod;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class OverridesMethodElementMatcher implements ElementMatcher<MethodDescription.InDefinedShape> {

	private final ElementMatcher<? super MethodDescription> extraMethodMatcher;
	private final ElementMatcher<? super TypeDescription> superClassMatcher;

	public static OverridesMethodElementMatcher overridesSuperMethod() {
		return new OverridesMethodElementMatcher();
	}

	public static OverridesMethodElementMatcher overridesSuperMethodThat(ElementMatcher<? super MethodDescription> methodElementMatcher) {
		return new OverridesMethodElementMatcher(methodElementMatcher);
	}

	private OverridesMethodElementMatcher() {
		this(any());
	}

	private OverridesMethodElementMatcher(ElementMatcher<? super MethodDescription> extraMethodMatcher) {
		this(extraMethodMatcher, not(is(TypeDescription.ForLoadedType.OBJECT)));
	}

	private OverridesMethodElementMatcher(ElementMatcher<? super MethodDescription> extraMethodMatcher, ElementMatcher<? super TypeDescription> superClassMatcher) {
		this.extraMethodMatcher = extraMethodMatcher;
		this.superClassMatcher = superClassMatcher;
	}

	public ElementMatcher<MethodDescription.InDefinedShape> onSuperClassesThat(ElementMatcher<? super TypeDescription> superClassMatcher) {
		return new OverridesMethodElementMatcher(extraMethodMatcher, superClassMatcher);
	}

	@Override
	public boolean matches(MethodDescription.InDefinedShape targetMethod) {
		TypeDescription superClass = targetMethod.getDeclaringType();
		do {
			superClass = superClass.getSuperClass().asErasure();
			if (declaresMethod(named(targetMethod.getName())
					.and(returns(targetMethod.getReturnType().asErasure()))
					.and(takesArguments(targetMethod.getParameters().asTypeList().asErasures()))
					.and(extraMethodMatcher))
					.matches(superClass)) {
				return true;
			}
		} while (superClassMatcher.matches(superClass));
		return false;
	}
}
