package org.stagemonitor.core.pool;

import java.util.List;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge;

public final class PooledResourceMetricsRegisterer {

	private PooledResourceMetricsRegisterer() {
	}

	public static void registerPooledResources(MetricRegistry registry, List<? extends PooledResource> pooledResources) {
		for (PooledResource pooledResource : pooledResources) {
			registerPooledResource(pooledResource, registry);
		}
	}

	public static void registerPooledResource(final PooledResource pooledResource, MetricRegistry registry) {
		String name = pooledResource.getName();
		registry.register(name + ".active", new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return pooledResource.getPoolNumActive();
			}
		});
		registry.register(name + ".count", new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return pooledResource.getActualPoolSize();
			}
		});
		registry.register(name + ".max", new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return pooledResource.getMaxPoolSize();
			}
		});
		if (pooledResource.getNumTasksPending() != null) {
			registry.register(name + ".queued", new Gauge<Integer>() {
				@Override
				public Integer getValue() {
					return pooledResource.getNumTasksPending();
				}
			});
		}
		registry.register(name + ".usage", new RatioGauge() {
			@Override
			protected Ratio getRatio() {
				return Ratio.of(pooledResource.getPoolNumActive(), pooledResource.getMaxPoolSize());
			}
		});
	}
}
