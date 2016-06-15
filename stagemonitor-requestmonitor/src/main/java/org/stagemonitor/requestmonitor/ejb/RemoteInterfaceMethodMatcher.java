package org.stagemonitor.requestmonitor.ejb;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static org.stagemonitor.requestmonitor.ejb.IsDeclaredInInterfaceHierarchyElementMatcher.isDeclaredInInterfaceHierarchy;

import javax.ejb.Remote;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

class RemoteInterfaceMethodMatcher extends ElementMatcher.Junction.AbstractBase<MethodDescription.InDefinedShape> {

	static AbstractBase<MethodDescription.InDefinedShape> overridesMethodFromInterfaceWhichIsAnnotatedWithRemote() {
		return new RemoteInterfaceMethodMatcher();
	}

	@Override
	public boolean matches(MethodDescription.InDefinedShape target) {
		return target.getDeclaringType().getInterfaces().asErasures()
				.filter(isAnnotatedWith(Remote.class))
				.filter(isDeclaredInInterfaceHierarchy(target)).size() > 0;
	}
}
