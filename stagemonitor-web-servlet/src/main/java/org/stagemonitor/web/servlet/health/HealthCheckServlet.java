package org.stagemonitor.web.servlet.health;

import com.codahale.metrics.health.HealthCheckRegistry;

import org.stagemonitor.core.util.JsonUtils;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HealthCheckServlet extends HttpServlet {

	private final HealthCheckRegistry healthCheckRegistry;

	public HealthCheckServlet(HealthCheckRegistry healthCheckRegistry) {
		this.healthCheckRegistry = healthCheckRegistry;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		JsonUtils.writeJsonToOutputStream(healthCheckRegistry.runHealthChecks(), resp.getOutputStream());
	}
}
