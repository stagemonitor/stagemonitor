package org.stagemonitor.requestmonitor.ejb;

import static org.stagemonitor.requestmonitor.ejb.IsDeclaredInInterfaceHierarchyElementMatcher.isDeclaredInInterfaceHierarchy;

import javax.ejb.Remote;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.matcher.ElementMatcher;

class IsDeclaredInRemoteClassElementMatcher extends ElementMatcher.Junction.AbstractBase<MethodDescription.InDefinedShape> {

	static AbstractBase<MethodDescription.InDefinedShape> overridesMethodFromRemoteAnnotationValue() {
		return new IsDeclaredInRemoteClassElementMatcher();
	}

	@Override
	public boolean matches(MethodDescription.InDefinedShape targetMethod) {
		final AnnotationList declaredAnnotationsOfType = targetMethod.getDeclaringType().getDeclaredAnnotations();
		if (declaredAnnotationsOfType.isAnnotationPresent(Remote.class)) {
			final Class[] remoteInterfaces = declaredAnnotationsOfType.ofType(Remote.class).loadSilent().value();
			if (!new TypeList.ForLoadedTypes(remoteInterfaces).filter(isDeclaredInInterfaceHierarchy(targetMethod)).isEmpty()) {
				return true;
			}
		}
		return false;
	}
}
