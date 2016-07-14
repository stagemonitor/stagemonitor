package org.stagemonitor.core.instrument;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.pool.TypePool;

import org.stagemonitor.core.util.ExecutorUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This {@link net.bytebuddy.agent.builder.AgentBuilder.TypeLocator} caches
 * {@link net.bytebuddy.description.type.TypeDescription}s and clears the cache every minute to avoid memory leaks.
 * <p/>
 * Class loader memory leaks are also avoided by using {@link WeakConcurrentMap}.
 */
public class AutoEvictingCachingBinaryLocator extends AgentBuilder.TypeLocator.WithTypePoolCache {

	private final WeakConcurrentMap<ClassLoader, TypePool.CacheProvider> cacheProviders = new WeakConcurrentMap
			.WithInlinedExpunction<ClassLoader, TypePool.CacheProvider>();
	private final ScheduledExecutorService executorService;

	public AutoEvictingCachingBinaryLocator() {
		this(TypePool.Default.ReaderMode.EXTENDED);
	}

	public AutoEvictingCachingBinaryLocator(TypePool.Default.ReaderMode readerMode) {
		super(readerMode);
		executorService = Executors.newScheduledThreadPool(1, new ExecutorUtils.NamedThreadFactory("type-pool-cache-evicter"));
		executorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				cacheProviders.clear();
				TimedElementMatcherDecorator.logMetrics();
			}
		}, 5, 1, TimeUnit.MINUTES);
	}

	@Override
	protected TypePool.CacheProvider locate(ClassLoader classLoader) {
		classLoader = classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader;
		TypePool.CacheProvider cacheProvider = cacheProviders.get(classLoader);
		while (cacheProvider == null) {
			cacheProvider = TypePool.CacheProvider.Simple.withObjectType();
			TypePool.CacheProvider previous = cacheProviders.putIfAbsent(classLoader, cacheProvider);
			if (previous != null) {
				cacheProvider = previous;
			}
		}
		return cacheProvider;
	}

	/**
	 * Shuts down the internal thread pool
	 */
	public void close() {
		executorService.shutdown();
	}

}
