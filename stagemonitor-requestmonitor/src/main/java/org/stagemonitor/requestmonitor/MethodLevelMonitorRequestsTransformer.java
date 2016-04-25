package org.stagemonitor.requestmonitor;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static org.stagemonitor.core.instrument.OverridesMethodElementMatcher.overridesSuperMethodThat;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class MethodLevelMonitorRequestsTransformer extends AbstractMonitorRequestsTransformer {

	@Override
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return isAnnotatedWith(MonitorRequests.class)
				// TODO maybe add a configuration to disable super method search as it is relatively costly
				// InstrumentationPerformanceTest without: ~20ms with: ~420ms
				// 0,5s is relatively much compared to other matchers but not really noticeable on startup
				.or(overridesSuperMethodThat(isAnnotatedWith(MonitorRequests.class)));
	}

}
