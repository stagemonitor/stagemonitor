package org.stagemonitor.web.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.StageMonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.SimpleSource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ConfigurationServlet extends HttpServlet {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Configuration configuration;

	public ConfigurationServlet(Configuration configuration) {
		this.configuration = configuration;
		configuration.addConfigurationSource(new SimpleSource(), true);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		reloadConfigIfRequested(req);
		resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	private boolean reloadConfigIfRequested(HttpServletRequest req) {
		if (req.getParameter("reload") != null) {
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

		String configurationUpdatePassword = req.getParameter(StageMonitor.STAGEMONITOR_PASSWORD);
		if (configurationUpdatePassword == null) {
			configurationUpdatePassword = "";
		}
		final String key = req.getParameter("key");
		final String configurationSource = req.getParameter("configurationSource");
		if (key != null && configurationSource != null) {
			tryToSaveAndHandleErrors(req, resp, configurationUpdatePassword, key, configurationSource);
		} else {
			if (key == null) {
				sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing parameter 'key'");
			} else {
				sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing parameter 'configurationSource'");
			}
		}
	}

	private void tryToSaveAndHandleErrors(HttpServletRequest req, HttpServletResponse resp, String configurationUpdatePassword, String key, String configurationSource) throws IOException {
		try {
			configuration.save(key, req.getParameter("value"), configurationSource, configurationUpdatePassword);
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

	private void sendError(HttpServletResponse response, int status, String message) throws IOException {
		response.setStatus(status);
		response.getWriter().print(message);
	}

}
