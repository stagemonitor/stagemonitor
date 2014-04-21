package org.stagemonitor.collector.core.monitor.rest;

import org.stagemonitor.collector.core.rest.RestClient;
import org.stagemonitor.collector.core.monitor.ExecutionContext;

public class ExecutionContextRestClient {

	public void saveRequestContext(String measurementSessionLocation, ExecutionContext requestContext) {
		if (measurementSessionLocation != null && !measurementSessionLocation.isEmpty()) {
			RestClient.sendAsJsonAsync(measurementSessionLocation + "/executionContexts", "POST", requestContext);
		}
	}
}
