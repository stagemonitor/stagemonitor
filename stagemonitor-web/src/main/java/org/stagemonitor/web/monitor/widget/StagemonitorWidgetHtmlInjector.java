package org.stagemonitor.web.monitor.widget;

import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.alerter.AlertSender;
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
	private AlertingPlugin alertingPlugin;
	private WebPlugin webPlugin;
	private Configuration configuration;
	private String widgetTemplate;

	@Override
	public void init(Configuration configuration, ServletContext servletContext) {
		this.configuration = configuration;
		this.webPlugin = configuration.getConfig(WebPlugin.class);
		alertingPlugin = configuration.getConfig(AlertingPlugin.class);
		try {
			this.widgetTemplate = buildWidgetTemplate(servletContext.getContextPath());
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
	public boolean isActive(HttpServletRequest httpServletRequest) {
		return webPlugin.isWidgetAndStagemonitorEndpointsAllowed(httpServletRequest, configuration);
	}

	@Override
	public String getContentToInjectBeforeClosingBody(RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation) {
		HttpRequestTrace requestTrace = requestInformation.getRequestTrace();
		AlertSender alertSender = alertingPlugin.getAlertSender();
		return widgetTemplate.replace("@@JSON_REQUEST_TACE_PLACEHOLDER@@", requestTrace != null ? requestTrace.toJson() : "null")
				.replace("@@CONFIGURATION_OPTIONS@@", JsonUtils.toJson(configuration.getConfigurationOptionsByCategory()))
				.replace("@@CONFIGURATION_PWD_SET@@", Boolean.toString(configuration.isPasswordSet()))
				.replace("@@CONFIGURATION_SOURCES@@", JsonUtils.toJson(configuration.getNamesOfConfigurationSources()))
				.replace("@@MEASUREMENT_SESSION@@", JsonUtils.toJson(Stagemonitor.getMeasurementSession()))
				.replace("@@ALERTER_TYPES@@", alertSender != null ? JsonUtils.toJson(alertSender.getAvailableAlerters()): "null")
				.replace("@@PATHS_OF_WIDGET_METRIC_TAB_PLUGINS@@", JsonUtils.toJson(Stagemonitor.getPathsOfWidgetMetricTabPlugins()));
	}
}
