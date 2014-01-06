package de.isys.jawap.collector.web.rest;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import de.isys.jawap.entities.profiler.ExecutionContext;
import de.isys.jawap.entities.web.HttpRequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.message.GZipEncoder;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;

public class HttpRequestContextRestClient {
	private final Log logger = LogFactory.getLog(getClass());

	private Client client;

	public HttpRequestContextRestClient() {
		client = ClientBuilder.newClient(new ClientConfig()
				.register(JacksonJsonProvider.class));
	}

	public void saveRequestContext(String measurementSessionLocation, ExecutionContext requestContext) {
		if (measurementSessionLocation != null && !measurementSessionLocation.isEmpty()) {
			WebTarget target = null;
			if (requestContext instanceof HttpRequestContext) {
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
