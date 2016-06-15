package org.stagemonitor.requestmonitor.ejb;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

class IsDeclaredInInterfaceHierarchyElementMatcher implements ElementMatcher<TypeDescription> {
	private final MethodDescription.InDefinedShape targetMethod;

	static ElementMatcher<TypeDescription> isDeclaredInInterfaceHierarchy(MethodDescription.InDefinedShape method) {
		return new IsDeclaredInInterfaceHierarchyElementMatcher(method);
	}

	public IsDeclaredInInterfaceHierarchyElementMatcher(MethodDescription.InDefinedShape targetMethod) {
		this.targetMethod = targetMethod;
	}

	@Override
	public boolean matches(TypeDescription targetInterface) {
		if (ElementMatchers.declaresMethod(named(targetMethod.getName())
				.and(returns(targetMethod.getReturnType().asErasure()))
				.and(takesArguments(targetMethod.getParameters().asTypeList().asErasures())))
				.matches(targetInterface)) {
			return true;
		} else {
			for (TypeDescription typeDescription : targetInterface.getInterfaces().asErasures()) {
				if (matches(typeDescription)) {
					return true;
				}
			}
		}
		return false;
	}
}
