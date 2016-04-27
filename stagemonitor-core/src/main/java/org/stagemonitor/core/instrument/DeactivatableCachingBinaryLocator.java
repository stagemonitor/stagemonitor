package org.stagemonitor.core.instrument;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicBoolean;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.pool.TypePool;

/**
 * This {@link net.bytebuddy.agent.builder.AgentBuilder.BinaryLocator} is intended to cache
 * {@link net.bytebuddy.description.type.TypeDescription}s only on application startup.
 * <p/>
 * After the the majority of the transformations are done (for example after the application has started)
 * one should call {@link #deactivateCaching()} to free memory.
 */
public class DeactivatableCachingBinaryLocator extends AgentBuilder.BinaryLocator.WithTypePoolCache {

	private final AtomicBoolean cacheEnabled = new AtomicBoolean(true);

	private final WeakConcurrentMap<ClassLoader, TypePool.CacheProvider> cacheProviders = new WeakConcurrentMap
			.WithInlinedExpunction<ClassLoader, TypePool.CacheProvider>();

	public DeactivatableCachingBinaryLocator() {
		this(TypePool.Default.ReaderMode.EXTENDED);
	}

	public DeactivatableCachingBinaryLocator(TypePool.Default.ReaderMode readerMode) {
		super(readerMode);
	}

	@Override
	protected TypePool.CacheProvider locate(ClassLoader classLoader) {
		if (cacheEnabled.get()) {
			classLoader = classLoader == null ? BootstrapClassLoaderMarker.INSTANCE : classLoader;
			TypePool.CacheProvider cacheProvider = cacheProviders.get(classLoader);
			while (cacheProvider == null) {
				cacheProviders.putIfAbsent(classLoader, new TypePool.CacheProvider.Simple());
				cacheProvider = cacheProviders.get(classLoader);
			}
			return cacheProvider;
		} else {
			return TypePool.CacheProvider.NoOp.INSTANCE;
		}
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

	public void deactivateCaching() {
		cacheEnabled.set(false);
		cacheProviders.clear();
	}

}
