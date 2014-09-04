package org.stagemonitor.web.configuration;

import org.stagemonitor.core.Configuration;
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
		dynamicConfigurationSource = new DynamicConfigurationSource(configuration);
		configuration.addConfigurationSource(dynamicConfigurationSource, true);

	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		reloadConfigIfRequested(req);
	}

	private void reloadConfigIfRequested(HttpServletRequest req) {
		if (req.getParameter("stagemonitorReloadConfig") != null) {
			configuration.reload();
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		reloadConfigIfRequested(req);

		String configurationUpdatePassword = req.getParameter(WebPlugin.STAGEMONITOR_PASSWORD);
		if (configurationUpdatePassword == null) {
			configurationUpdatePassword = "";
		}
		final String key = req.getParameter("stagemonitorConfigKey");
		if (key != null) {
			if (!dynamicConfigurationSource.updateConfiguration(key, req.getParameter("stagemonitorConfigValue"), configurationUpdatePassword)) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			}
		}
		resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

}
