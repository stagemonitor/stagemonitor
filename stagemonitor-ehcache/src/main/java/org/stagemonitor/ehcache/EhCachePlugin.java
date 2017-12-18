package org.stagemonitor.ehcache;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.grafana.GrafanaClient;
import org.stagemonitor.core.metrics.health.ImmediateResult;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.FirstOperationEventListener;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import java.util.Collections;
import java.util.List;

public class EhCachePlugin extends StagemonitorPlugin {

	private static final Logger logger = LoggerFactory.getLogger(EhCachePlugin.class);

	private final ConfigurationOption<String> ehCacheNameOption = ConfigurationOption.stringOption()
			.key("stagemonitor.ehcache.name")
			.dynamic(false)
			.label("EhCache cache name")
			.description("The name of the ehcache to instrument (the value of the `name` attribute of the " +
					"`ehcache` tag in ehcache.xml)")
			.configurationCategory("EhCache Plugin")
			.build();
	private final ConfigurationOption<Boolean> timeGet = ConfigurationOption.booleanOption()
			.key("stagemonitor.ehcache.get.timer")
			.dynamic(true)
			.label("Create timer for cache gets")
			.description("If set to true, a timer for each cache will be created which measures the time to get a " +
					"element from the cache. If you have a lot of caches, that could lead to a increased network and " +
					"disk utilisation. If set to false, only a meter (which measures the rate) will be created")
			.configurationCategory("EhCache Plugin")
			.buildWithDefault(false);

	private Metric2Registry metricRegistry;
	private HealthCheckRegistry healthCheckRegistry;

	/*
	 * TODO monitor caches by instrumenting the constructor of net.sf.ehcache.Cache
	 *
	 * handle the case where one constructor calls another -> make sure cache is not monitored twice
	 */
	@Override
	public void initializePlugin(StagemonitorPlugin.InitArguments initArguments) {
		this.metricRegistry = initArguments.getMetricRegistry();
		healthCheckRegistry = initArguments.getHealthCheckRegistry();
		TracingPlugin tracingPlugin = initArguments.getPlugin(TracingPlugin.class);
		final CacheManager cacheManager = CacheManager.getCacheManager(ehCacheNameOption.getValue());
		if (cacheManager == null) {
			tryAgainWhenFirstRequestComesIn(tracingPlugin);
		} else {
			monitorCaches(cacheManager);
			healthCheckRegistry.register("EhCache CacheManager", ImmediateResult.of(HealthCheck.Result.healthy()));
		}


	}

	private void tryAgainWhenFirstRequestComesIn(TracingPlugin tracingPlugin) {
		logger.info("Can't monitor EhCache as the CacheManager could not be found. " +
				"Check the config for 'stagemonitor.ehcache.name'. " +
				"It could also be possible that cache manager is not initialized yet. " +
				"Stagemonitor will try to initialize again when the application serves the first request.");
		tracingPlugin.addSpanEventListenerFactory(new FirstOperationEventListener(tracingPlugin.getSpanWrappingTracer()) {
			@Override
			public void onFirstOperation(SpanWrapper spanWrapper) {
				final CacheManager cacheManager = CacheManager.getCacheManager(ehCacheNameOption.getValue());
				final HealthCheck check;
				if (cacheManager != null) {
					monitorCaches(cacheManager);
					check = ImmediateResult.of(HealthCheck.Result.healthy("CacheManager found after first operation"));
				} else {
					check = ImmediateResult.of(HealthCheck.Result.unhealthy("CacheManager not found after first operation"));
				}
				healthCheckRegistry.register("EhCache CacheManager", check);
			}

			@Override
			public boolean customCondition(SpanWrapper spanWrapper) {
				// TODO what if the application is not a server application?
				return SpanUtils.isServerRequest(spanWrapper);
			}
		});
	}

	@Override
	public List<Class<? extends StagemonitorPlugin>> dependsOn() {
		return Collections.<Class<? extends StagemonitorPlugin>>singletonList(TracingPlugin.class);
	}

	/**
	 * You can use this method to manually register a {@link CacheManager} that should be monitored by stagemonitor.
	 * <p>
	 * Example:
	 * <pre>
	 * Stagemonitor.getPlugin(EhCachePlugin.class).monitorCaches(yourCacheManager);
	 * </pre>
	 *
	 * @param cacheManager The CacheManager to monitor
	 */
	public void monitorCaches(CacheManager cacheManager) {
		if (cacheManager == null) {
			logger.warn("EhCache can't be monitored; CacheManager is null");
			return;
		}
		for (String cacheName : cacheManager.getCacheNames()) {
			monitorCache(cacheManager.getCache(cacheName));
		}
	}

	/**
	 * You can use this method to manually register a {@link Cache} that should be monitored by stagemonitor.
	 * <p>
	 * Example:
	 * <pre>
	 * Stagemonitor.getPlugin(EhCachePlugin.class).monitorCache(yourCache);
	 * </pre>
	 *
	 * @param cache The Cache to monitor
	 */
	public void monitorCache(Ehcache cache) {
		cache.setStatisticsEnabled(true);
		final StagemonitorCacheUsageListener cacheUsageListener = new StagemonitorCacheUsageListener(cache.getName(),
				metricRegistry, timeGet.getValue());
		cache.registerCacheUsageListener(cacheUsageListener);
		metricRegistry.registerAny(new EhCacheMetricSet(cache.getName(), cache, cacheUsageListener));
	}

	@Override
	public void registerWidgetMetricTabPlugins(WidgetMetricTabPluginsRegistry widgetMetricTabPluginsRegistry) {
		widgetMetricTabPluginsRegistry.addWidgetMetricTabPlugin("/stagemonitor/static/tabs/metrics/ehcache-metrics");
	}

}
