package org.stagemonitor.web.configuration;

import org.stagemonitor.core.Configuration;
import org.stagemonitor.web.WebPlugin;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

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
		super.doGet(req, resp);    //To change body of overridden methods use File | Settings | File Templates.
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		updateConfiguration(req);
		resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	private void updateConfiguration(HttpServletRequest httpServletRequest) {
		@SuppressWarnings("unchecked")
		final Map<String, String[]> parameterMap = httpServletRequest.getParameterMap();

		final String configurationUpdatePassword = getFirstOrEmpty(parameterMap.get(WebPlugin.STAGEMONITOR_PASSWORD));
		for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
			if ("stagemonitorReloadConfig".equals(entry.getKey())) {
				configuration.reload();
			} else if (entry.getKey().startsWith("stagemonitor.")) {
				dynamicConfigurationSource.updateConfiguration(entry.getKey(), getFirstOrEmpty(entry.getValue()), configurationUpdatePassword);
			}
		}
	}

	private String getFirstOrEmpty(String[] strings) {
		if (strings != null && strings.length > 0) {
			return strings[0];
		}
		return "";
	}

}
