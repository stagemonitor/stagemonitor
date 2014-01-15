package de.isys.jawap.collector.web.rest;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import de.isys.jawap.entities.profiler.ExecutionContext;
import de.isys.jawap.entities.web.HttpExecutionContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

public class HttpExecutionContextRestClient {
	private final Log logger = LogFactory.getLog(getClass());

	private Client client;

	public HttpExecutionContextRestClient() {
		client = ClientBuilder.newClient(new ClientConfig()
				.register(JacksonJsonProvider.class));
	}

	public void saveRequestContext(String measurementSessionLocation, ExecutionContext requestContext) {
		if (measurementSessionLocation != null && !measurementSessionLocation.isEmpty()) {
			WebTarget target = null;
			if (requestContext instanceof HttpExecutionContext) {
				target = client.target(measurementSessionLocation).path("/executionContexts");
			}
			if (target == null) {
				logger.error("Unknown request context: " + requestContext.getClass().getCanonicalName());
			} else {
				target.request().async().post(Entity.json(requestContext));
			}
		}
	}
}
