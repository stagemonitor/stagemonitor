package org.stagemonitor.web.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(ConfigurationServlet.CONFIGURATION_ENDPOINT)
public class ConfigurationServlet extends HttpServlet {

	public static final String CONFIGURATION_ENDPOINT = "/stagemonitor/configuration";

	private static final Logger logger = LoggerFactory.getLogger(ConfigurationServlet.class);

	private final Configuration configuration;

	public ConfigurationServlet() {
		this(Stagemonitor.getConfiguration());
	}

	public ConfigurationServlet(Configuration configuration) {
		this.configuration = configuration;
		logger.info("Registering configuration Endpoint {}. You can dynamically change the configuration by " +
				"issuing a POST request to {}?key=stagemonitor.config.key&value=configValue&stagemonitor.password=password. " +
				"If the password is not set, dynamically changing the configuration is not available. " +
				"The password can be omitted if set to an empty string.",
				ConfigurationServlet.CONFIGURATION_ENDPOINT, ConfigurationServlet.CONFIGURATION_ENDPOINT);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		reloadConfigIfRequested(req);
		resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	private boolean reloadConfigIfRequested(HttpServletRequest req) {
		if (req.getParameter("reload") != null) {
			configuration.reloadDynamicConfigurationOptions();
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

		final String key = req.getParameter("key");
		if (key == null) {
			sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing parameter 'key'");
			return;
		}
		final String configurationSource = req.getParameter("configurationSource");
		if (configurationSource == null) {
			sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing parameter 'configurationSource'");
			return;
		}
		tryToSaveAndHandleErrors(configuration, req, resp, req.getParameter(Stagemonitor.STAGEMONITOR_PASSWORD), key,
				req.getParameter("value"));
	}

	public static void tryToSaveAndHandleErrors(Configuration configuration, HttpServletRequest req,
												HttpServletResponse resp, String configurationUpdatePassword,
												String key, String value) throws IOException {
		try {
			configuration.save(key, value, req.getParameter("configurationSource"), configurationUpdatePassword);
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		} catch (IllegalArgumentException e) {
			sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		} catch (IllegalStateException e) {
			sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
		} catch (UnsupportedOperationException e) {
			sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Error. Check your server logs.");
		}
	}

	private static void sendError(HttpServletResponse response, int status, String message) throws IOException {
		response.setStatus(status);
		response.getWriter().print(message);
	}

}
