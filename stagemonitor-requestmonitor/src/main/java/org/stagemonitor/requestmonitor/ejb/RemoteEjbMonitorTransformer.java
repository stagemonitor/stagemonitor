package org.stagemonitor.requestmonitor.ejb;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static org.stagemonitor.requestmonitor.ejb.IsDeclaredInRemoteClassElementMatcher.overridesMethodFromRemoteAnnotationValue;
import static org.stagemonitor.requestmonitor.ejb.RemoteInterfaceElementMatcher.implementsInterfaceWhichIsAnnotatedWithRemote;
import static org.stagemonitor.requestmonitor.ejb.RemoteInterfaceMethodMatcher.overridesMethodFromInterfaceWhichIsAnnotatedWithRemote;

import javax.ejb.Remote;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.requestmonitor.AbstractMonitorRequestsTransformer;

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
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
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
