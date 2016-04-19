package org.stagemonitor.core.instrument;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.core.util.ClassUtils;

public class CachedClassLoaderMatcher extends ElementMatcher.Junction.AbstractBase<ClassLoader> {

	private final ConcurrentMap<String, Boolean> cache = new ConcurrentHashMap<String, Boolean>();

	private final ElementMatcher<ClassLoader> delegate;

	public static ElementMatcher.Junction.AbstractBase<ClassLoader> cached(ElementMatcher<ClassLoader> delegate) {
		return new CachedClassLoaderMatcher(delegate);
	}

	private CachedClassLoaderMatcher(ElementMatcher<ClassLoader> delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean matches(ClassLoader target) {
		final String key = ClassUtils.getIdentityString(target);
		final Boolean result = cache.get(key);
		if (result != null) {
			return result;
		} else {
			final boolean delegateResult = delegate.matches(target);
			cache.put(key, delegateResult);
			return delegateResult;
		}
	}
}
