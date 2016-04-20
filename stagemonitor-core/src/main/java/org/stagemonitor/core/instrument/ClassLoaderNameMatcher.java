package org.stagemonitor.core.instrument;

import net.bytebuddy.matcher.ElementMatcher;

public class ClassLoaderNameMatcher extends ElementMatcher.Junction.AbstractBase<ClassLoader> {

	private final String name;

	private ClassLoaderNameMatcher(String name) {
		this.name = name;
	}

	public static ElementMatcher.Junction.AbstractBase<ClassLoader> classLoaderWithName(String name) {
		return new ClassLoaderNameMatcher(name);

	}

	public static ElementMatcher.Junction.AbstractBase<ClassLoader> isReflectionClassLoader() {
		return new ClassLoaderNameMatcher("sun.reflect.DelegatingClassLoader");
	}

	@Override
	public boolean matches(ClassLoader target) {
		return target != null && name.equals(target.getClass().getName());
	}
}
