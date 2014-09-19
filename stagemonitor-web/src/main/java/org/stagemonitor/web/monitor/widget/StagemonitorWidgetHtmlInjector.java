package org.stagemonitor.web.monitor.widget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.IOUtils;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.HttpRequestTrace;
import org.stagemonitor.web.monitor.filter.HtmlInjector;

import java.io.IOException;

public class StagemonitorWidgetHtmlInjector implements HtmlInjector {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final WebPlugin webPlugin;
	private final Configuration configuration;
	private String widgetTemplate;

	public StagemonitorWidgetHtmlInjector(Configuration configuration, WebPlugin webPlugin, String contextPath) {
		this.configuration = configuration;
		this.webPlugin = webPlugin;
		try {
			this.widgetTemplate = buildWidgetTemplate(contextPath);
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
			this.widgetTemplate = "";
		}
	}

	private String buildWidgetTemplate(String contextPath) throws IOException {
		return IOUtils.toString(getClass().getClassLoader().getResourceAsStream("stagemonitorWidget.html"))
				.replace("@@CONTEXT_PREFIX_PATH@@", contextPath);
	}


	@Override
	public boolean isActive() {
		return webPlugin.isWidgetEnabled();
	}

	@Override
	public String build(RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation) {
		return widgetTemplate.replace("@@JSON_REQUEST_TACE_PLACEHOLDER@@", requestInformation.getRequestTrace().toJson())
				.replace("@@CONFIGURATION_OPTIONS@@", JsonUtils.toJson(configuration.getConfigurationOptionsByPlugin()))
				.replace("@@CONFIGURATION_PWD_SET@@", Boolean.toString(configuration.isPasswordSet()))
				.replace("@@CONFIGURATION_SOURCES@@", JsonUtils.toJson(configuration.getNamesOfConfigurationSources()));
	}
}
