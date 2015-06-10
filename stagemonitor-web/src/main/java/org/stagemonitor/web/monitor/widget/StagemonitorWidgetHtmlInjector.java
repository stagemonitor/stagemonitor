package org.stagemonitor.web.monitor.widget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.IOUtils;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.HttpRequestTrace;
import org.stagemonitor.web.monitor.filter.HtmlInjector;

public class StagemonitorWidgetHtmlInjector implements HtmlInjector {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private WebPlugin webPlugin;
	private Configuration configuration;
	private String widgetTemplate;
	private String contextPath;

	@Override
	public void init(Configuration configuration, ServletContext servletContext) {
		this.configuration = configuration;
		this.webPlugin = configuration.getConfig(WebPlugin.class);
		try {
			contextPath = servletContext.getContextPath();
			this.widgetTemplate = buildWidgetTemplate(contextPath);
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
			this.widgetTemplate = "";
		}
	}

	private String buildWidgetTemplate(String contextPath) throws IOException {
		return IOUtils.getResourceAsString("stagemonitorWidget.html")
				.replace("@@CONTEXT_PREFIX_PATH@@", contextPath);
	}

	@Override
	public boolean isActive(HttpServletRequest httpServletRequest) {
		return webPlugin.isWidgetAndStagemonitorEndpointsAllowed(httpServletRequest, configuration);
	}

	@Override
	public String getContentToInjectBeforeClosingBody(RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation) {
		HttpRequestTrace requestTrace = null;
		if (requestInformation != null) {
			requestTrace = requestInformation.getRequestTrace();
		}
		final List<String> pathsOfWidgetTabPlugins = new ArrayList<String>();
		for (String path : Stagemonitor.getPathsOfWidgetTabPlugins()) {
			pathsOfWidgetTabPlugins.add(contextPath + path);
		}

		final List<String> pathsOfWidgetMetricTabPlugins = new ArrayList<String>();
		for (String path : Stagemonitor.getPathsOfWidgetMetricTabPlugins()) {
			pathsOfWidgetMetricTabPlugins.add(contextPath + path);
		}

		return widgetTemplate.replace("@@JSON_REQUEST_TACE_PLACEHOLDER@@", requestTrace != null ? requestTrace.toJson() : "null")
				.replace("@@CONFIGURATION_OPTIONS@@", JsonUtils.toJson(configuration.getConfigurationOptionsByCategory()))
				.replace("@@CONFIGURATION_PWD_SET@@", Boolean.toString(configuration.isPasswordSet()))
				.replace("@@CONFIGURATION_SOURCES@@", JsonUtils.toJson(configuration.getNamesOfConfigurationSources()))
				.replace("@@MEASUREMENT_SESSION@@", JsonUtils.toJson(Stagemonitor.getMeasurementSession()))
				.replace("@@PATHS_OF_TAB_PLUGINS@@", JsonUtils.toJson(pathsOfWidgetTabPlugins))
				.replace("@@PATHS_OF_WIDGET_METRIC_TAB_PLUGINS@@", JsonUtils.toJson(pathsOfWidgetMetricTabPlugins));
	}
}
