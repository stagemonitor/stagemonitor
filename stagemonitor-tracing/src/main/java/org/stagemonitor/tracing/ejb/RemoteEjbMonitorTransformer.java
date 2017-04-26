package org.stagemonitor.tracing.ejb;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.tracing.AbstractMonitorRequestsTransformer;

import javax.ejb.Remote;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static org.stagemonitor.tracing.ejb.IsDeclaredInRemoteClassElementMatcher.overridesMethodFromRemoteAnnotationValue;
import static org.stagemonitor.tracing.ejb.RemoteInterfaceElementMatcher.implementsInterfaceWhichIsAnnotatedWithRemote;
import static org.stagemonitor.tracing.ejb.RemoteInterfaceMethodMatcher.overridesMethodFromInterfaceWhichIsAnnotatedWithRemote;

/**
 * This class is responsible for detecting EJB remote calls
 * <p/>
 * It detects classes that are either annotated with @{@link Remote} or implement an interface which is annotated with @{@link Remote}
 */
public class RemoteEjbMonitorTransformer extends AbstractMonitorRequestsTransformer {

	@Override
	protected ElementMatcher.Junction<TypeDescription> getNarrowTypesMatcher() {
		return isAnnotatedWith(Remote.class).or(implementsInterfaceWhichIsAnnotatedWithRemote());
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription> getExtraMethodElementMatcher() {
		return isPublic().and(
				overridesMethodFromRemoteAnnotationValue()
						.or(overridesMethodFromInterfaceWhichIsAnnotatedWithRemote())
		);
	}

	@Override
	public boolean isActive() {
		return ClassUtils.isPresent("javax.ejb.Remote");
	}

}
