package org.stagemonitor.core.instrument;

import net.bytebuddy.matcher.ElementMatcher;

public class CachedClassLoaderMatcher extends ElementMatcher.Junction.AbstractBase<ClassLoader> {

	private final WeakConcurrentMap<ClassLoader, Boolean> cache = new WeakConcurrentMap.WithInlinedExpunction<ClassLoader, Boolean>();

	private final ElementMatcher<ClassLoader> delegate;

	public static ElementMatcher.Junction.AbstractBase<ClassLoader> cached(ElementMatcher<ClassLoader> delegate) {
		return new CachedClassLoaderMatcher(delegate);
	}

	private CachedClassLoaderMatcher(ElementMatcher<ClassLoader> delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean matches(ClassLoader target) {
		final Boolean result = cache.get(target);
		if (result != null) {
			return result;
		} else {
			final boolean delegateResult = delegate.matches(target);
			cache.put(target, delegateResult);
			return delegateResult;
		}
	}
}
