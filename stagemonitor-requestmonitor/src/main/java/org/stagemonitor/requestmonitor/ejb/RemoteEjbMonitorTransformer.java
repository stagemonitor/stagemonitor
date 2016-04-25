package org.stagemonitor.requestmonitor.ejb;

import static net.bytebuddy.matcher.ElementMatchers.declaresMethod;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import javax.ejb.Remote;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.requestmonitor.AbstractMonitorRequestsTransformer;

public class RemoteEjbMonitorTransformer extends AbstractMonitorRequestsTransformer {

	@Override
	protected ElementMatcher.Junction<TypeDescription> getIncludeTypeMatcher() {
		return super.getIncludeTypeMatcher().and(isAnnotatedWith(Remote.class));
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
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
			return declaredAnnotationsOfType.isAnnotationPresent(Remote.class) &&
					!new TypeList.ForLoadedTypes(declaredAnnotationsOfType.ofType(Remote.class).loadSilent().value())
							.filter(new IsDeclaredInElementMatcher(targetMethod)).isEmpty();
		}

	}

	private static class IsDeclaredInElementMatcher implements ElementMatcher<TypeDescription> {
		private final MethodDescription.InDefinedShape targetMethod;

		public IsDeclaredInElementMatcher(MethodDescription.InDefinedShape targetMethod) {
			this.targetMethod = targetMethod;
		}

		@Override
		public boolean matches(TypeDescription target) {
			return declaresMethod(named(targetMethod.getName())
					.and(returns(targetMethod.getReturnType().asErasure()))
					.and(takesArguments(targetMethod.getParameters().asTypeList().asErasures())))
					.matches(target);
		}
	}
}
