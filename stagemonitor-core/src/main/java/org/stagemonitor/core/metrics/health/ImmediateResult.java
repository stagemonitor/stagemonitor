package org.stagemonitor.core.metrics.health;

import com.codahale.metrics.health.HealthCheck;

public class ImmediateResult extends HealthCheck {

	private final Result result;

	public static HealthCheck of(Result result) {
		return new ImmediateResult(result);
	}

	private ImmediateResult(Result result) {
		this.result = result;
	}

	@Override
	protected Result check() throws Exception {
		return result;
	}
}
