package org.stagemonitor.requestmonitor.ejb;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

import javax.ejb.Remote;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

class RemoteInterfaceElementMatcher implements ElementMatcher<TypeDescription> {

	static ElementMatcher<TypeDescription> implementsInterfaceWhichIsAnnotatedWithRemote() {
		return new RemoteInterfaceElementMatcher();
	}

	@Override
	public boolean matches(TypeDescription target) {
		return !target.getInterfaces().asErasures().filter(isAnnotatedWith(Remote.class)).isEmpty();
	}
}
