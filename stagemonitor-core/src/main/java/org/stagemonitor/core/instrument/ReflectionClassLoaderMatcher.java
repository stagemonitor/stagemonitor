package org.stagemonitor.core.instrument;

import net.bytebuddy.matcher.ElementMatcher;

public class ReflectionClassLoaderMatcher extends ElementMatcher.Junction.AbstractBase<ClassLoader> {

	public static ElementMatcher.Junction.AbstractBase<ClassLoader> isReflectionClassLoader() {
		return new ReflectionClassLoaderMatcher();
	}

	@Override
	public boolean matches(ClassLoader target) {
		return target != null && "sun.reflect.DelegatingClassLoader".equals(target.getClass().getName());
	}
}
