package org.stagemonitor.requestmonitor.ejb;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;

import javax.ejb.Remote;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.requestmonitor.AbstractMonitorRequestsTransformer;

public class RemoteEjbMonitorTransformer extends AbstractMonitorRequestsTransformer {

	@Override
	protected ElementMatcher.Junction<TypeDescription> getIncludeTypeMatcher() {
		return super.getIncludeTypeMatcher().and(isAnnotatedWith(Remote.class));
	}

	@Override
	protected ElementMatcher.Junction<? super MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return isPublic().and(new IsDeclaredInRemoteClassElementMatcher());
	}

	@Override
	public boolean isActive() {
		return ClassUtils.isPresent("javax.ejb.Remote");
	}

	private static class IsDeclaredInRemoteClassElementMatcher extends ElementMatcher.Junction.AbstractBase<MethodDescription.InDefinedShape> {
		@Override
		public boolean matches(MethodDescription.InDefinedShape targetMethod) {
			final AnnotationList declaredAnnotationsOfType = targetMethod.getDeclaringType().getDeclaredAnnotations();
			if (!declaredAnnotationsOfType.isAnnotationPresent(Remote.class)) {
				return false;
			}
			final Remote annotation = declaredAnnotationsOfType.ofType(Remote.class).loadSilent();
			return isDeclaredInAnyRemoteInterface(targetMethod, annotation.value());
		}

		private boolean isDeclaredInAnyRemoteInterface(MethodDescription.InDefinedShape targetMethod, Class[] remoteInterfaces) {
			for (Class<?> remoteInterface : remoteInterfaces) {
				if (isDeclaredIn(targetMethod, remoteInterface)) {
					return true;
				}
			}
			return false;
		}

		private boolean isDeclaredIn(final MethodDescription.InDefinedShape targetMethod, Class<?> remoteInterface) {
			final TypeDescription.ForLoadedType remoteInterfaceType = new TypeDescription.ForLoadedType(remoteInterface);
			return remoteInterfaceType
					.getDeclaredMethods()
					.filter(new ElementMatcher<MethodDescription.InDefinedShape>() {
				@Override
				public boolean matches(MethodDescription.InDefinedShape target) {
					return target.getName().equals(targetMethod.getName()) &&
							target.getDescriptor().equals(targetMethod.getDescriptor());
				}
			}).size() > 0;
		}
	}
}
