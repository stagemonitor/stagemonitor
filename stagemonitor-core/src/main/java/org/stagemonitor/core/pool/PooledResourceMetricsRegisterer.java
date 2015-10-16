package org.stagemonitor.core.pool;

import java.util.List;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.RatioGauge;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;

public final class PooledResourceMetricsRegisterer {

	private PooledResourceMetricsRegisterer() {
	}

	public static void registerPooledResources(Metric2Registry registry, List<? extends PooledResource> pooledResources) {
		for (PooledResource pooledResource : pooledResources) {
			registerPooledResource(pooledResource, registry);
		}
	}

	public static void registerPooledResource(final PooledResource pooledResource, Metric2Registry registry) {
		MetricName name = pooledResource.getName();
		registry.register(name.withTag("type", "active"), new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return pooledResource.getPoolNumActive();
			}
		});
		registry.register(name.withTag("type", "count"), new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return pooledResource.getActualPoolSize();
			}
		});
		registry.register(name.withTag("type", "max"), new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return pooledResource.getMaxPoolSize();
			}
		});
		if (pooledResource.getNumTasksPending() != null) {
			registry.register(name.withTag("type", "queued"), new Gauge<Integer>() {
				@Override
				public Integer getValue() {
					return pooledResource.getNumTasksPending();
				}
			});
		}
		registry.register(name.withTag("type", "usage"), new RatioGauge() {
			@Override
			protected Ratio getRatio() {
				return Ratio.of(pooledResource.getPoolNumActive() * 100.0, pooledResource.getMaxPoolSize());
			}
		});
	}
}
