package org.stagemonitor.core;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class CorePlugin implements StageMonitorPlugin {

	private static final String CORE_PLUGIN_NAME = "Core";

	final ConfigurationOption<Boolean> stagemonitorActive = ConfigurationOption.booleanOption()
			.key("stagemonitor.active")
			.dynamic(true)
			.label("Activate stagemonitor")
			.description("If set to 'false' stagemonitor will be completely deactivated.")
			.defaultValue(true)
			.pluginName(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Boolean> internalMonitoring = ConfigurationOption.booleanOption()
			.key("stagemonitor.internal.monitoring")
			.dynamic(true)
			.label("Internal monitoring")
			.description("If active, stagemonitor will collect internal performance data")
			.defaultValue(false)
			.pluginName(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Integer> reportingIntervalConsole = ConfigurationOption.integerOption()
			.key("stagemonitor.reporting.interval.console")
			.dynamic(false)
			.label("Reporting interval console")
			.description("The amount of time between console reports (in seconds). " +
					"To deactivate console reports, set this to a value below 1.")
			.defaultValue(60)
			.pluginName(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Boolean> reportingJmx = ConfigurationOption.booleanOption()
			.key("stagemonitor.reporting.jmx")
			.dynamic(false)
			.label("Expose MBeans")
			.description("Whether or not to expose all metrics as MBeans.")
			.defaultValue(true)
			.pluginName(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Integer> reportingIntervalGraphite = ConfigurationOption.integerOption()
			.key("stagemonitor.reporting.interval.graphite")
			.dynamic(false)
			.label("Reporting interval graphite")
			.description("The amount of time between the metrics are reported to graphite (in seconds).\n" +
					"To deactivate graphite reporting, set this to a value below 1, or don't provide " +
					"stagemonitor.reporting.graphite.hostName.")
			.defaultValue(60)
			.pluginName(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<String> graphiteHostName = ConfigurationOption.stringOption()
			.key("stagemonitor.reporting.graphite.hostName")
			.dynamic(false)
			.label("Graphite host name")
			.description("The name of the host where graphite is running. This setting is mandatory, if you want " +
					"to use the grafana dashboards.")
			.defaultValue(null)
			.pluginName(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Integer> graphitePort = ConfigurationOption.integerOption()
			.key("stagemonitor.reporting.graphite.port")
			.dynamic(false)
			.label("Carbon port")
			.description("The port where carbon is listening.")
			.defaultValue(2003)
			.pluginName(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<String> applicationName = ConfigurationOption.stringOption()
			.key("stagemonitor.applicationName")
			.dynamic(false)
			.label("Application name")
			.description("The name of the application.\n" +
					"Either this property or the display-name in web.xml is mandatory!")
			.defaultValue(null)
			.pluginName(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<String> instanceName = ConfigurationOption.stringOption()
			.key("stagemonitor.instanceName")
			.dynamic(false)
			.label("Instance name")
			.description("The instance name.\n" +
					"If this property is not set, the instance name set to the first request's " +
					"javax.servlet.ServletRequest#getServerName()\n" +
					"That means that the collection of metrics does not start before the first request is executed!")
			.defaultValue(null)
			.pluginName(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<String> elasticsearchUrl = ConfigurationOption.stringOption()
			.key("stagemonitor.elasticsearch.url")
			.dynamic(true)
			.label("Elasticsearch URL")
			.description("The URL of the elasticsearch server that stores the call stacks. If the URL is not " +
					"provided, the call stacks won't get stored.")
			.defaultValue(null)
			.pluginName(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<List<Pattern>> excludedMetrics = ConfigurationOption.regexListOption()
			.key("stagemonitor.metrics.excluded.pattern")
			.dynamic(false)
			.label("Excluded metrics (regex)")
			.description("A comma separated list of metric names that should not be collected.")
			.defaultValue(Collections.<Pattern>emptyList())
			.pluginName(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Collection<String>> disabledPlugins = ConfigurationOption.stringsOption()
			.key("stagemonitor.plugins.disabled")
			.dynamic(false)
			.label("Disabled plugins")
			.description("A comma separated list of plugin names (the simple class name) that should not be active.")
			.defaultValue(Collections.<String>emptyList())
			.pluginName(CORE_PLUGIN_NAME)
			.build();

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
	}

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		return Arrays.<ConfigurationOption<?>>asList(stagemonitorActive, internalMonitoring, reportingIntervalConsole,
				reportingJmx, reportingIntervalGraphite, graphiteHostName, graphitePort, applicationName, instanceName,
				elasticsearchUrl, excludedMetrics, disabledPlugins);
	}

	public boolean isStagemonitorActive() {
		return stagemonitorActive.getValue();
	}

	public boolean isInternalMonitoringActive() {
		return internalMonitoring.getValue();
	}

	public long getConsoleReportingInterval() {
		return reportingIntervalConsole.getValue();
	}

	public boolean reportToJMX() {
		return reportingJmx.getValue();
	}

	public int getGraphiteReportingInterval() {
		return reportingIntervalGraphite.getValue();
	}

	public String getGraphiteHostName() {
		return graphiteHostName.getValue();
	}

	public int getGraphitePort() {
		return graphitePort.getValue();
	}

	public String getApplicationName() {
		return applicationName.getValue();
	}

	public String getInstanceName() {
		return instanceName.getValue();
	}

	public String getElasticsearchUrl() {
		final String url = elasticsearchUrl.getValue();
		if (url != null && url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}

	public Collection<Pattern> getExcludedMetricsPatterns() {
		return excludedMetrics.getValue();
	}

	public Collection<String> getDisabledPlugins() {
		return disabledPlugins.getValue();
	}
}
