package org.stagemonitor.web.servlet.widget;

import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.tracing.reporter.ReadbackSpan;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.util.IOUtils;
import org.stagemonitor.web.servlet.ServletPlugin;
import org.stagemonitor.web.servlet.filter.HtmlInjector;

import java.util.ArrayList;
import java.util.List;

public class StagemonitorWidgetHtmlInjector extends HtmlInjector {

	/**
	 * Whether the in browser widget should be opened automatically
	 * (without needing to click on the speaker icon)
	 */
	private final boolean openImmediately;
	private ServletPlugin servletPlugin;
	private ConfigurationRegistry configuration;
	private String widgetTemplate;
	private String contextPath;

	public StagemonitorWidgetHtmlInjector() {
		this(false);
	}

	public StagemonitorWidgetHtmlInjector(boolean openImmediately) {
		this.openImmediately = openImmediately;
	}

	@Override
	public void init(HtmlInjector.InitArguments initArguments) {
		this.configuration = initArguments.getConfiguration();
		this.servletPlugin = initArguments.getConfiguration().getConfig(ServletPlugin.class);
		contextPath = initArguments.getServletContext().getContextPath();
		this.widgetTemplate = buildWidgetTemplate(contextPath);
	}

	private String buildWidgetTemplate(String contextPath) {
		return IOUtils.getResourceAsString("stagemonitorWidget.html")
				.replace("@@CONTEXT_PREFIX_PATH@@", contextPath)
				.replace("@@OPEN_IMMEDIATELY@@", Boolean.toString(openImmediately))
				.replace("@@OVERLAY_DISPLAY@@", openImmediately ? "block" : "none");
	}

	@Override
	public boolean isActive(HtmlInjector.IsActiveArguments isActiveArguments) {
		return servletPlugin.isWidgetAndStagemonitorEndpointsAllowed(isActiveArguments.getHttpServletRequest(), configuration);
	}

	@Override
	public void injectHtml(HtmlInjector.InjectArguments injectArguments) {
		ReadbackSpan span = null;
		if (injectArguments.getSpanContext() != null) {
			span = injectArguments.getSpanContext().getReadbackSpan();
		}
		final List<String> pathsOfWidgetTabPlugins = new ArrayList<String>();
		for (String path : Stagemonitor.getPathsOfWidgetTabPlugins()) {
			pathsOfWidgetTabPlugins.add(contextPath + path);
		}

		final List<String> pathsOfWidgetMetricTabPlugins = new ArrayList<String>();
		for (String path : Stagemonitor.getPathsOfWidgetMetricTabPlugins()) {
			pathsOfWidgetMetricTabPlugins.add(contextPath + path);
		}

		injectArguments.setContentToInjectBeforeClosingBody(widgetTemplate
				.replace("@@JSON_REQUEST_TACE_PLACEHOLDER@@", span != null ? JsonUtils.toJson(span, SpanUtils.CALL_TREE_ASCII) : "null")
				.replace("@@CONFIGURATION_OPTIONS@@", JsonUtils.toJson(configuration.getConfigurationOptionsByCategory()))
				.replace("@@CONFIGURATION_PWD_SET@@", Boolean.toString(configuration.isPasswordSet()))
				.replace("@@CONFIGURATION_SOURCES@@", JsonUtils.toJson(configuration.getNamesOfConfigurationSources()))
				.replace("@@MEASUREMENT_SESSION@@", JsonUtils.toJson(Stagemonitor.getMeasurementSession()))
				.replace("@@PATHS_OF_TAB_PLUGINS@@", JsonUtils.toJson(pathsOfWidgetTabPlugins))
				.replace("@@PATHS_OF_WIDGET_METRIC_TAB_PLUGINS@@", JsonUtils.toJson(pathsOfWidgetMetricTabPlugins)));
	}
}
