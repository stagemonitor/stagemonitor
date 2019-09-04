package org.stagemonitor.tracing.ejb;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.matcher.ElementMatcher;

import javax.ejb.Remote;

import static org.stagemonitor.tracing.ejb.IsDeclaredInInterfaceHierarchyElementMatcher.isDeclaredInInterfaceHierarchy;

class IsDeclaredInRemoteClassElementMatcher extends ElementMatcher.Junction.AbstractBase<MethodDescription> {

	static AbstractBase<MethodDescription> overridesMethodFromRemoteAnnotationValue() {
		return new IsDeclaredInRemoteClassElementMatcher();
	}

	@Override
	public boolean matches(MethodDescription targetMethod) {
		final AnnotationList declaredAnnotationsOfType = targetMethod.getDeclaringType().asErasure().getDeclaredAnnotations();
		if (declaredAnnotationsOfType.isAnnotationPresent(Remote.class)) {
			final Class[] remoteInterfaces = declaredAnnotationsOfType.ofType(Remote.class).load().value();
			if (!new TypeList.ForLoadedTypes(remoteInterfaces).filter(isDeclaredInInterfaceHierarchy(targetMethod)).isEmpty()) {
				return true;
			}
		}
		return false;
	}
}
