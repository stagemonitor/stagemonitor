package org.stagemonitor.requestmonitor;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class MethodLevelMonitorRequestsTransformer extends AbstractMonitorRequestsTransformer {

	@Override
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return isAnnotatedWith(MonitorRequests.class);
	}

}
