package org.stagemonitor.core.instrument;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.declaresMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class OverridesMethodElementMatcher implements ElementMatcher<MethodDescription.InDefinedShape> {

	private final ElementMatcher<? super MethodDescription> extraMethodMatcher;

	public static ElementMatcher<MethodDescription.InDefinedShape> overridesSuperMethod() {
		return new OverridesMethodElementMatcher();
	}
	public static ElementMatcher<MethodDescription.InDefinedShape> overridesSuperMethodThat(ElementMatcher<? super MethodDescription> methodElementMatcher) {
		return new OverridesMethodElementMatcher(methodElementMatcher);
	}

	private OverridesMethodElementMatcher() {
		extraMethodMatcher = any();
	}

	private OverridesMethodElementMatcher(ElementMatcher<? super MethodDescription> extraMethodMatcher) {
		this.extraMethodMatcher = extraMethodMatcher;
	}

	@Override
	public boolean matches(MethodDescription.InDefinedShape targetMethod) {
		TypeDefinition superClass = targetMethod.getDeclaringType();
		while (!superClass.equals(TypeDescription.ForLoadedType.OBJECT)) {
			superClass = superClass.getSuperClass();
			if (declaresMethod(named(targetMethod.getName())
					.and(returns(targetMethod.getReturnType().asErasure()))
					.and(takesArguments(targetMethod.getParameters().asTypeList().asErasures()))
					.and(extraMethodMatcher))
					.matches(superClass)) {
				return true;
			}
		}
		return false;
	}
}
