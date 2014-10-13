package org.stagemonitor.junit;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

public class ConditionalTravisTestRunner extends BlockJUnit4ClassRunner {

	public ConditionalTravisTestRunner(Class clazz) throws InitializationError {
		super(clazz);
	}

	@Override
	public void runChild(FrameworkMethod method, RunNotifier notifier) {
		ExcludeOnTravis condition = method.getAnnotation(ExcludeOnTravis.class);
		if (condition == null) {
			super.runChild(method, notifier);
		} else if ("true".equals(System.getenv("TRAVIS"))) {
			notifier.fireTestIgnored(describeChild(method));
		} else {
			super.runChild(method, notifier);
		}
	}
}
