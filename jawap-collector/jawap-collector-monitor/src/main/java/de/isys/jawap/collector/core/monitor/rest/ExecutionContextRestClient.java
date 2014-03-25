package de.isys.jawap.collector.core.monitor.rest;

import de.isys.jawap.collector.core.rest.RestClient;
import de.isys.jawap.entities.profiler.ExecutionContext;

public class ExecutionContextRestClient {

	public void saveRequestContext(String measurementSessionLocation, ExecutionContext requestContext) {
		if (measurementSessionLocation != null && !measurementSessionLocation.isEmpty()) {
			RestClient.sendAsJsonAsync(measurementSessionLocation + "/executionContexts", "POST", requestContext);
		}
	}
}
