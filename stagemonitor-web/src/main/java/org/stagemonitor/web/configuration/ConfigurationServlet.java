package org.stagemonitor.web.configuration;

import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.DynamicConfigurationSource;
import org.stagemonitor.web.WebPlugin;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ConfigurationServlet extends HttpServlet {

	private final Configuration configuration;
	private final DynamicConfigurationSource dynamicConfigurationSource;


	public ConfigurationServlet(Configuration configuration) {
		this.configuration = configuration;
		dynamicConfigurationSource = new DynamicConfigurationSource(configuration, WebPlugin.STAGEMONITOR_PASSWORD);
		configuration.addConfigurationSource(dynamicConfigurationSource, true);

	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		reloadConfigIfRequested(req);
		resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	private boolean reloadConfigIfRequested(HttpServletRequest req) {
		if (req.getParameter("stagemonitorReloadConfig") != null) {
			configuration.reload();
			return true;
		}
		return false;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (reloadConfigIfRequested(req)) {
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return;
		}

		String configurationUpdatePassword = req.getParameter(WebPlugin.STAGEMONITOR_PASSWORD);
		if (configurationUpdatePassword == null) {
			configurationUpdatePassword = "";
		}
		final String key = req.getParameter("stagemonitorConfigKey");
		if (key != null) {
			try {
				dynamicConfigurationSource.updateConfiguration(key, req.getParameter("stagemonitorConfigValue"), configurationUpdatePassword);
				resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
			} catch (IllegalArgumentException e) {
				sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			} catch (IllegalStateException e) {
				sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
			}
		} else {
			sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing parameter stagemonitorConfigKey");
		}
	}

	private void sendError(HttpServletResponse response, int status, String message) throws IOException {
		response.setStatus(status);
		response.getWriter().print(message);
	}

}
