package org.stagemonitor.core.instrument;

import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.core.util.ClassUtils;

/**
 * Matches those {@link ClassLoader}s which are able to load a particular class
 */
public class CanLoadClassElementMatcher implements ElementMatcher<ClassLoader> {

	private final String className;

	public static ElementMatcher<ClassLoader> canLoadClass(String className) {
		return new CanLoadClassElementMatcher(className);
	}

	private CanLoadClassElementMatcher(String className) {
		this.className = className;
	}

	@Override
	public boolean matches(ClassLoader target) {
		return ClassUtils.canLoadClass(target, className);
	}
}
