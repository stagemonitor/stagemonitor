package org.stagemonitor.requestmonitor.ejb;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

import javax.ejb.Remote;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static org.stagemonitor.requestmonitor.ejb.IsDeclaredInInterfaceHierarchyElementMatcher.isDeclaredInInterfaceHierarchy;

class RemoteInterfaceMethodMatcher extends ElementMatcher.Junction.AbstractBase<MethodDescription> {

	static AbstractBase<MethodDescription> overridesMethodFromInterfaceWhichIsAnnotatedWithRemote() {
		return new RemoteInterfaceMethodMatcher();
	}

	@Override
	public boolean matches(MethodDescription target) {
		return target.getDeclaringType().getInterfaces().asErasures()
				.filter(isAnnotatedWith(Remote.class))
				.filter(isDeclaredInInterfaceHierarchy(target)).size() > 0;
	}
}
