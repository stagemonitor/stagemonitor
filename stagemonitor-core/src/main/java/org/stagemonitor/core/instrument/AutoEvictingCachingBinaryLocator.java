package org.stagemonitor.core.instrument;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.pool.TypePool;
import org.stagemonitor.core.util.ExecutorUtils;

/**
 * This {@link net.bytebuddy.agent.builder.AgentBuilder.TypeLocator} caches
 * {@link net.bytebuddy.description.type.TypeDescription}s and clears the cache every minute to avoid memory leaks.
 * <p/>
 * Class loader memory leaks are also avoided by using {@link WeakConcurrentMap}.
 */
public class AutoEvictingCachingBinaryLocator extends AgentBuilder.TypeLocator.WithTypePoolCache {

	private final WeakConcurrentMap<ClassLoader, TypePool.CacheProvider> cacheProviders = new WeakConcurrentMap
			.WithInlinedExpunction<ClassLoader, TypePool.CacheProvider>();

	public AutoEvictingCachingBinaryLocator() {
		this(TypePool.Default.ReaderMode.EXTENDED);
	}

	public AutoEvictingCachingBinaryLocator(TypePool.Default.ReaderMode readerMode) {
		super(readerMode);
		Executors.newScheduledThreadPool(1, new ExecutorUtils.NamedThreadFactory("type-pool-cache-evicter")).scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				cacheProviders.clear();
				TimedElementMatcherDecorator.logMetrics();
			}
		}, 5, 1, TimeUnit.MINUTES);
	}

	@Override
	protected TypePool.CacheProvider locate(ClassLoader classLoader) {
		classLoader = classLoader == null ? BootstrapClassLoaderMarker.INSTANCE : classLoader;
		TypePool.CacheProvider cacheProvider = cacheProviders.get(classLoader);
		while (cacheProvider == null) {
			cacheProviders.putIfAbsent(classLoader, new TypePool.CacheProvider.Simple());
			cacheProvider = cacheProviders.get(classLoader);
		}
		return cacheProvider;
	}

	/**
	 * A marker for the bootstrap class loader which is represented by {@code null}.
	 */
	private static class BootstrapClassLoaderMarker extends ClassLoader {

		/**
		 * A static reference to the a singleton instance of the marker to preserve reference equality.
		 */
		protected static final ClassLoader INSTANCE = AccessController.doPrivileged(new CreationAction());

		@Override
		protected Class<?> loadClass(String name, boolean resolve) {
			throw new UnsupportedOperationException("This loader is only a non-null marker and is not supposed to be used");
		}

		/**
		 * A simple action for creating a bootstrap class loader marker.
		 */
		private static class CreationAction implements PrivilegedAction<ClassLoader> {

			@Override
			public ClassLoader run() {
				return new BootstrapClassLoaderMarker();
			}
		}
	}


}
