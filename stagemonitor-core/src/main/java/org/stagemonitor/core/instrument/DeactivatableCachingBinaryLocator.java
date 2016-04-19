package org.stagemonitor.core.instrument;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.pool.TypePool;

/**
 * This {@link net.bytebuddy.agent.builder.AgentBuilder.BinaryLocator} is intended to cache
 * {@link net.bytebuddy.description.type.TypeDescription}s only on application startup.
 * <p/>
 * After the application has started one MUST call {@link #deactivateCaching()} to avoid {@link ClassLoader} memory leaks.
 */
public class DeactivatableCachingBinaryLocator extends AgentBuilder.BinaryLocator.WithTypePoolCache.Simple {

	private final AtomicBoolean cacheEnabled = new AtomicBoolean(true);

	private final ConcurrentHashMap<ClassLoader, TypePool.CacheProvider> cacheProviders;

	public DeactivatableCachingBinaryLocator() {
		this(TypePool.Default.ReaderMode.FAST, new ConcurrentHashMap<ClassLoader, TypePool.CacheProvider>());
	}

	public DeactivatableCachingBinaryLocator(TypePool.Default.ReaderMode readerMode,
											 ConcurrentHashMap<ClassLoader, TypePool.CacheProvider> cacheProviders) {
		super(readerMode, cacheProviders);
		this.cacheProviders = cacheProviders;
	}

	@Override
	protected TypePool.CacheProvider locate(ClassLoader classLoader) {
		if (cacheEnabled.get()) {
			return super.locate(classLoader);
		} else {
			return TypePool.CacheProvider.NoOp.INSTANCE;
		}
	}

	public void deactivateCaching() {
		cacheEnabled.set(false);
		cacheProviders.clear();
	}

}
