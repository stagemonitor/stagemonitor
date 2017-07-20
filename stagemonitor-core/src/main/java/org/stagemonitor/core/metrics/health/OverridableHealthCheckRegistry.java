package org.stagemonitor.core.metrics.health;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;

public class OverridableHealthCheckRegistry extends HealthCheckRegistry {

	@Override
	public void register(String name, HealthCheck healthCheck) {
		super.unregister(name);
		super.register(name, healthCheck);
	}

}
