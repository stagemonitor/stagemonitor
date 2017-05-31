package org.stagemonitor.tracing;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.stagemonitor.core.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static org.stagemonitor.core.instrument.OverridesMethodElementMatcher.overridesSuperMethodThat;
import static org.stagemonitor.core.instrument.StagemonitorClassNameMatcher.isInsideMonitoredProject;

public class MethodLevelTracingTransformer extends AbstractTracingTransformer {

	private final Set<Class<? extends Annotation>> asyncCallAnnotations = new HashSet<Class<? extends Annotation>>();

	@SuppressWarnings("unchecked")
	public MethodLevelTracingTransformer() {
		final TracingPlugin tracingPlugin = configuration.getConfig(TracingPlugin.class);
		if (tracingPlugin.isMonitorAsyncInvocations()) {
			asyncCallAnnotations.add((Class<? extends Annotation>) ClassUtils.forNameOrNull("org.springframework.scheduling.annotation.Async"));
			asyncCallAnnotations.add((Class<? extends Annotation>) ClassUtils.forNameOrNull("javax.ejb.Asynchronous"));
		}
		if (tracingPlugin.isMonitorScheduledTasks()) {
			asyncCallAnnotations.add((Class<? extends Annotation>) ClassUtils.forNameOrNull("org.springframework.scheduling.annotation.Scheduled"));
			asyncCallAnnotations.add((Class<? extends Annotation>) ClassUtils.forNameOrNull("org.springframework.scheduling.annotation.Schedules"));
			asyncCallAnnotations.add((Class<? extends Annotation>) ClassUtils.forNameOrNull("javax.ejb.Schedule"));
			asyncCallAnnotations.add((Class<? extends Annotation>) ClassUtils.forNameOrNull("javax.ejb.Schedules"));
		}
		asyncCallAnnotations.remove(null);
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription> getExtraMethodElementMatcher() {
		ElementMatcher.Junction<MethodDescription> matcher = isAnnotatedWith(Traced.class)
				// TODO maybe add a configuration to disable super method search as it is relatively costly
				// InstrumentationPerformanceTest without: ~20ms with: ~420ms
				// 0,5s is relatively much compared to other matchers but not really noticeable on startup
				.or(overridesSuperMethodThat(isAnnotatedWith(Traced.class)).onSuperClassesThat(isInsideMonitoredProject()));

		for (Class<? extends Annotation> annotation : asyncCallAnnotations) {
			matcher = matcher.or(isAnnotatedWith(annotation));
		}

		return matcher;
	}

}
