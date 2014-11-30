package org.stagemonitor.core;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.MetricsAggregationReporter;
import org.stagemonitor.core.metrics.MetricsWithCountFilter;
import org.stagemonitor.core.metrics.OrMetricFilter;
import org.stagemonitor.core.metrics.RegexMetricFilter;
import org.stagemonitor.core.metrics.SortedTableLogReporter;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.codahale.metrics.MetricRegistry.name;
import static org.stagemonitor.core.util.GraphiteSanitizer.sanitizeGraphiteMetricSegment;

/**
 * This class contains the configuration options for stagemonitor's core functionality
 */
public class CorePlugin extends StagemonitorPlugin {

	private static final String CORE_PLUGIN_NAME = "Core";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	final ConfigurationOption<Boolean> stagemonitorActive = ConfigurationOption.booleanOption()
			.key("stagemonitor.active")
			.dynamic(true)
			.label("Activate stagemonitor")
			.description("If set to `false` stagemonitor will be completely deactivated.")
			.defaultValue(true)
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Boolean> internalMonitoring = ConfigurationOption.booleanOption()
			.key("stagemonitor.internal.monitoring")
			.dynamic(true)
			.label("Internal monitoring")
			.description("If active, stagemonitor will collect internal performance data")
			.defaultValue(false)
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Integer> reportingIntervalConsole = ConfigurationOption.integerOption()
			.key("stagemonitor.reporting.interval.console")
			.dynamic(false)
			.label("Reporting interval console")
			.description("The amount of time between console reports (in seconds). " +
					"To deactivate console reports, set this to a value below 1.")
			.defaultValue(60)
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Integer> reportingIntervalAggregation = ConfigurationOption.integerOption()
			.key("stagemonitor.reporting.interval.aggregation")
			.dynamic(false)
			.label("Metrics aggregation interval")
			.description("The amount of time between all registered metrics are aggregated for a report on server " +
					"shutdown that shows aggregated values for all metrics of the measurement session. " +
					"To deactivate a aggregate report on shutdown, set this to a value below 1.")
			.defaultValue(30)
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Boolean> reportingJmx = ConfigurationOption.booleanOption()
			.key("stagemonitor.reporting.jmx")
			.dynamic(false)
			.label("Expose MBeans")
			.description("Whether or not to expose all metrics as MBeans.")
			.defaultValue(true)
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Integer> reportingIntervalGraphite = ConfigurationOption.integerOption()
			.key("stagemonitor.reporting.interval.graphite")
			.dynamic(false)
			.label("Reporting interval graphite")
			.description("The amount of time between the metrics are reported to graphite (in seconds).\n" +
					"To deactivate graphite reporting, set this to a value below 1, or don't provide " +
					"stagemonitor.reporting.graphite.hostName.")
			.defaultValue(60)
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<String> graphiteHostName = ConfigurationOption.stringOption()
			.key("stagemonitor.reporting.graphite.hostName")
			.dynamic(false)
			.label("Graphite host name")
			.description("The name of the host where graphite is running. This setting is mandatory, if you want " +
					"to use the grafana dashboards.")
			.defaultValue(null)
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Integer> graphitePort = ConfigurationOption.integerOption()
			.key("stagemonitor.reporting.graphite.port")
			.dynamic(false)
			.label("Carbon port")
			.description("The port where carbon is listening.")
			.defaultValue(2003)
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<String> applicationName = ConfigurationOption.stringOption()
			.key("stagemonitor.applicationName")
			.dynamic(false)
			.label("Application name")
			.description("The name of the application.\n" +
					"Either this property or the display-name in web.xml is mandatory!")
			.defaultValue(null)
			.configurationCategory(CORE_PLUGIN_NAME)
			.tags("important")
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
			.configurationCategory(CORE_PLUGIN_NAME)
			.tags("important")
			.build();
	private final ConfigurationOption<String> elasticsearchUrl = ConfigurationOption.stringOption()
			.key("stagemonitor.elasticsearch.url")
			.dynamic(true)
			.label("Elasticsearch URL")
			.description("The URL of the elasticsearch server that stores the call stacks. If the URL is not " +
					"provided, the call stacks won't get stored.")
			.defaultValue(null)
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Collection<String>> elasticsearchConfigurationSourceProfiles = ConfigurationOption.stringsOption()
			.key("stagemonitor.elasticsearch.configurationSourceProfiles")
			.dynamic(false)
			.label("Elasticsearch configuration source ids")
			.description("Set configuration profiles of configuration stored in elasticsearch as a centralized configuration source " +
					"that can be shared between multiple server instances. Set the profiles appropriate to the current " +
					"environment e.g. `production,common`, `local`, `test`, ... The configuration will be stored under " +
					"{stagemonitor.elasticsearch.url}/stagemonitor/configuration/{configurationSourceProfile}.")
			.defaultValue(Collections.<String>emptyList())
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Boolean> deactivateStagemonitorIfEsConfigSourceIsDown = ConfigurationOption.booleanOption()
			.key("stagemonitor.elasticsearch.configurationSource.deactivateStagemonitorIfEsIsDown")
			.dynamic(false)
			.label("Deactivate stagemonitor if elasticsearch configuration source is down")
			.description("Set to true if stagemonitor should be deactivated if " +
					"stagemonitor.elasticsearch.configurationSourceProfiles is set but elasticsearch can't be reached " +
					"under stagemonitor.elasticsearch.url. Defaults to true to prevent starting stagemonitor with " +
					"wrong configuration.")
			.defaultValue(true)
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<List<Pattern>> excludedMetrics = ConfigurationOption.regexListOption()
			.key("stagemonitor.metrics.excluded.pattern")
			.dynamic(false)
			.label("Excluded metrics (regex)")
			.description("A comma separated list of metric names that should not be collected.")
			.defaultValue(Collections.<Pattern>emptyList())
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Collection<String>> disabledPlugins = ConfigurationOption.stringsOption()
			.key("stagemonitor.plugins.disabled")
			.dynamic(false)
			.label("Disabled plugins")
			.description("A comma separated list of plugin names (the simple class name) that should not be active.")
			.defaultValue(Collections.<String>emptyList())
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Integer> reloadConfigurationInterval = ConfigurationOption.integerOption()
			.key("stagemonitor.configuration.reload.interval")
			.dynamic(false)
			.label("Configuration reload interval")
			.description("The interval in seconds a reload of all configuration sources is performed. " +
					"Set to a value below `1` to deactivate periodic reloading the configuration.")
			.defaultValue(60)
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private static MetricsAggregationReporter aggregationReporter;

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
		final CorePlugin corePlugin = configuration.getConfig(CorePlugin.class);
		final Integer reloadInterval = corePlugin.getReloadConfigurationInterval();
		if (reloadInterval > 0) {
			configuration.scheduleReloadAtRate(reloadInterval, TimeUnit.SECONDS);
		}

		ElasticsearchClient.sendGrafanaDashboardAsync("Custom Metrics.json");
		InputStream resourceAsStream = getClass().getClassLoader()
				.getResourceAsStream("stagemonitor-elasticsearch-configuration-index-template.json");
		ElasticsearchClient.sendAsJsonAsync("PUT", "/_template/stagemonitor-configuration", resourceAsStream);

		registerReporters(metricRegistry, corePlugin);
	}

	private void registerReporters(MetricRegistry metricRegistry, CorePlugin corePlugin) {
		RegexMetricFilter regexFilter = new RegexMetricFilter(corePlugin.getExcludedMetricsPatterns());
		metricRegistry.removeMatching(regexFilter);

		MetricFilter allFilters = new OrMetricFilter(regexFilter, new MetricsWithCountFilter());

		reportToGraphite(metricRegistry, corePlugin.getGraphiteReportingInterval(),
				Stagemonitor.getMeasurementSession(), allFilters, corePlugin);

		List<ScheduledReporter> onShutdownReporters = new LinkedList<ScheduledReporter>();
		reportToConsole(metricRegistry, corePlugin.getConsoleReportingInterval(), allFilters, onShutdownReporters);
		registerAggregationReporter(metricRegistry, allFilters, onShutdownReporters, corePlugin.getAggregationReportingInterval());
		if (corePlugin.reportToJMX()) {
			reportToJMX(metricRegistry, allFilters);
		}
	}

	private void registerAggregationReporter(MetricRegistry metricRegistry, MetricFilter allFilters,
											 List<ScheduledReporter> onShutdownReporters, long reportingInterval) {
		if (reportingInterval > 0) {
			aggregationReporter = new MetricsAggregationReporter(metricRegistry, allFilters, onShutdownReporters);
			aggregationReporter.start(reportingInterval, TimeUnit.SECONDS);
		}
	}

	private static void reportToGraphite(MetricRegistry metricRegistry, long reportingInterval,
										 MeasurementSession measurementSession,
										 MetricFilter filter, CorePlugin corePlugin) {
		String graphiteHostName = corePlugin.getGraphiteHostName();
		if (graphiteHostName != null && !graphiteHostName.isEmpty()) {
			GraphiteReporter.forRegistry(metricRegistry)
					.prefixedWith(getGraphitePrefix(measurementSession))
					.convertRatesTo(TimeUnit.SECONDS)
					.convertDurationsTo(TimeUnit.MILLISECONDS)
					.filter(filter)
					.build(new Graphite(new InetSocketAddress(graphiteHostName, corePlugin.getGraphitePort())))
					.start(reportingInterval, TimeUnit.SECONDS);
		}
	}

	private static String getGraphitePrefix(MeasurementSession measurementSession) {
		return name("stagemonitor",
				sanitizeGraphiteMetricSegment(measurementSession.getApplicationName()),
				sanitizeGraphiteMetricSegment(measurementSession.getInstanceName()),
				sanitizeGraphiteMetricSegment(measurementSession.getHostName()));
	}

	private static void reportToConsole(MetricRegistry metricRegistry, long reportingInterval, MetricFilter filter,
										List<ScheduledReporter> onShutdownReporters) {
		final SortedTableLogReporter reporter = SortedTableLogReporter.forRegistry(metricRegistry)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS)
				.filter(filter)
				.build();
		onShutdownReporters.add(reporter);
		if (reportingInterval > 0) {
			reporter.start(reportingInterval, TimeUnit.SECONDS);
		}
	}

	private static void reportToJMX(MetricRegistry metricRegistry, MetricFilter filter) {
		JmxReporter.forRegistry(metricRegistry)
				.filter(filter)
				.build().start();
	}

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		return Arrays.<ConfigurationOption<?>>asList(stagemonitorActive, internalMonitoring, reportingIntervalConsole,
				reportingJmx, reportingIntervalGraphite, graphiteHostName, graphitePort, applicationName, instanceName,
				elasticsearchUrl, elasticsearchConfigurationSourceProfiles, deactivateStagemonitorIfEsConfigSourceIsDown,
				excludedMetrics, disabledPlugins, reloadConfigurationInterval, reportingIntervalAggregation);
	}

	@Override
	public void onShutDown() {
		if (aggregationReporter != null) {
			logger.info("\n####################################################\n" +
					"## Aggregated report for this measurement session ##\n" +
					"####################################################\n");
			aggregationReporter.onShutDown();
		}
	}

	public boolean isStagemonitorActive() {
		return Stagemonitor.isDisabled() ? false : stagemonitorActive.getValue();
	}

	public boolean isInternalMonitoringActive() {
		return internalMonitoring.getValue();
	}

	public long getConsoleReportingInterval() {
		return reportingIntervalConsole.getValue();
	}


	private long getAggregationReportingInterval() {
		return reportingIntervalAggregation.getValue();
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

	public Collection<String> getElasticsearchConfigurationSourceProfiles() {
		return elasticsearchConfigurationSourceProfiles.getValue();
	}

	public boolean isDeactivateStagemonitorIfEsConfigSourceIsDown() {
		return deactivateStagemonitorIfEsConfigSourceIsDown.getValue();
	}

	public Collection<Pattern> getExcludedMetricsPatterns() {
		return excludedMetrics.getValue();
	}

	public Collection<String> getDisabledPlugins() {
		return disabledPlugins.getValue();
	}

	public Integer getReloadConfigurationInterval() {
		return reloadConfigurationInterval.getValue();
	}
}
