package org.stagemonitor.core;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.configuration.converter.ListValueConverter;
import org.stagemonitor.core.configuration.converter.SetValueConverter;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.elasticsearch.IndexSelector;
import org.stagemonitor.core.grafana.GrafanaClient;
import org.stagemonitor.core.metrics.MetricNameFilter;
import org.stagemonitor.core.metrics.MetricsAggregationReporter;
import org.stagemonitor.core.metrics.MetricsWithCountFilter;
import org.stagemonitor.core.metrics.SortedTableLogReporter;
import org.stagemonitor.core.metrics.metrics2.AndMetric2Filter;
import org.stagemonitor.core.metrics.metrics2.ElasticsearchReporter;
import org.stagemonitor.core.metrics.metrics2.InfluxDbReporter;
import org.stagemonitor.core.metrics.metrics2.Metric2Filter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.metrics.metrics2.MetricNameValueConverter;
import org.stagemonitor.core.metrics.metrics2.ScheduledMetrics2Reporter;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.IOUtils;
import org.stagemonitor.core.util.StringUtils;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;
import static org.stagemonitor.core.util.GraphiteSanitizer.sanitizeGraphiteMetricSegment;

/**
 * This class contains the configuration options for stagemonitor's core functionality
 */
public class CorePlugin extends StagemonitorPlugin {

	public static final String DEFAULT_APPLICATION_NAME = "My Application";

	private static final String CORE_PLUGIN_NAME = "Core";
	public static final String POOLS_QUEUE_CAPACITY_LIMIT_KEY = "stagemonitor.threadPools.queueCapacityLimit";
	private static final String ELASTICSEARCH = "elasticsearch";
	private static final String METRICS_STORE = "metrics-store";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final ConfigurationOption<Boolean> stagemonitorActive = ConfigurationOption.booleanOption()
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
			.defaultValue(0)
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
			.defaultValue(false)
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
			.tags(METRICS_STORE, "graphite")
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<String> graphiteHostName = ConfigurationOption.stringOption()
			.key("stagemonitor.reporting.graphite.hostName")
			.dynamic(false)
			.label("Graphite host name")
			.description("The name of the host where graphite is running. This setting is mandatory, if you want " +
					"to use the grafana dashboards.")
			.defaultValue(null)
			.tags(METRICS_STORE, "graphite")
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Integer> graphitePort = ConfigurationOption.integerOption()
			.key("stagemonitor.reporting.graphite.port")
			.dynamic(false)
			.label("Carbon port")
			.description("The port where carbon is listening.")
			.defaultValue(2003)
			.tags(METRICS_STORE, "graphite")
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<String> influxDbUrl = ConfigurationOption.stringOption()
			.key("stagemonitor.reporting.influxdb.url")
			.dynamic(true)
			.label("InfluxDB URL")
			.description("The URL of your InfluxDB installation.")
			.defaultValue(null)
			.tags(METRICS_STORE, "influx-db")
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<String> influxDbDb = ConfigurationOption.stringOption()
			.key("stagemonitor.reporting.influxdb.db")
			.dynamic(true)
			.label("InfluxDB database")
			.description("The target database")
			.defaultValue("stagemonitor")
			.tags(METRICS_STORE, "influx-db")
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Integer> reportingIntervalInfluxDb = ConfigurationOption.integerOption()
			.key("stagemonitor.reporting.interval.influxdb")
			.dynamic(false)
			.label("Reporting interval InfluxDb")
			.description("The amount of time between the metrics are reported to InfluxDB (in seconds).")
			.defaultValue(60)
			.tags(METRICS_STORE, "influx-db")
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Integer> reportingIntervalElasticsearch = ConfigurationOption.integerOption()
			.key("stagemonitor.reporting.interval.elasticsearch")
			.dynamic(false)
			.label("Reporting interval Elasticsearch")
			.description("The amount of time between the metrics are reported to Elasticsearch (in seconds).")
			.defaultValue(60)
			.tags(METRICS_STORE, ELASTICSEARCH)
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Boolean> onlyLogElasticsearchMetricReports = ConfigurationOption.booleanOption()
			.key("stagemonitor.reporting.elasticsearch.onlyLogElasticsearchMetricReports")
			.dynamic(false)
			.label("Only log Elasticsearch metric reports")
			.description(String.format("If set to true, the metrics won't be reported to elasticsearch but instead logged in bulk format. " +
					"The name of the logger is %s. That way you can redirect the reporting to a separate log file and use logstash or a " +
					"different external process to send the metrics to elasticsearch.", ElasticsearchReporter.ES_METRICS_LOGGER))
			.defaultValue(false)
			.tags(METRICS_STORE, ELASTICSEARCH)
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Integer> deleteElasticsearchMetricsAfterDays = ConfigurationOption.integerOption()
			.key("stagemonitor.reporting.elasticsearch.deleteMetricsAfterDays")
			.dynamic(false)
			.label("Delete ES metrics after (days)")
			.description("The number of days after the metrics stored in elasticsearch should be deleted. Set below 1 to deactivate.")
			.defaultValue(-1)
			.tags(METRICS_STORE, ELASTICSEARCH)
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Integer> moveToColdNodesAfterDays = ConfigurationOption.integerOption()
			.key("stagemonitor.elasticsearch.hotColdArchitecture.moveToColdNodesAfterDays")
			.dynamic(false)
			.label("Activate Hot-Cold Architecture")
			.description("Setting this to a value > 1 activates the hot-cold architecture described in https://www.elastic.co/blog/hot-warm-architecture " +
					"where new data that is more frequently queried and updated is stored on beefy nodes (SSDs, more RAM and CPU). " +
					"When the indexes reach a certain age, they are allocated on cold nodes. For this to work, you have to tag your " +
					"beefy nodes with node.box_type: hot (either in elasticsearch.yml or start the node using ./bin/elasticsearch --node.box_type hot)" +
					"and your historical nodes with node.box_type: cold.")
			.defaultValue(-1)
			.tags(METRICS_STORE, ELASTICSEARCH)
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Integer> numberOfReplicas = ConfigurationOption.integerOption()
			.key("stagemonitor.elasticsearch.numberOfReplicas")
			.dynamic(false)
			.label("Number of ES Replicas")
			.description("Sets the number of replicas of the Elasticsearch index templates.")
			.defaultValue(null)
			.tags(METRICS_STORE, ELASTICSEARCH)
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Integer> numberOfShards = ConfigurationOption.integerOption()
			.key("stagemonitor.elasticsearch.numberOfShards")
			.dynamic(false)
			.label("Number of ES Shards")
			.description("Sets the number of shards of the Elasticsearch index templates.")
			.defaultValue(null)
			.tags(METRICS_STORE, ELASTICSEARCH)
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
	private final ConfigurationOption<String> hostName = ConfigurationOption.stringOption()
			.key("stagemonitor.hostName")
			.dynamic(false)
			.label("Host name")
			.description("The host name.\n" +
					"If this property is not set, the host name will default to resolving the host name for localhost, " +
					"if this fails it will be loaded from the environment, either from COMPUTERNAME or HOSTNAME.")
			.defaultValue(getNameOfLocalHost())
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<List<String>> elasticsearchUrls = ConfigurationOption.builder(ListValueConverter.STRINGS_VALUE_CONVERTER, List.class)
			.key("stagemonitor.elasticsearch.url")
			.dynamic(true)
			.label("Elasticsearch URL")
			.description("A comma separated list of the Elasticsearch URLs that store spans and metrics. " +
					"If your ES cluster is secured with basic authentication, you can use urls like https://user:password@example.com.")
			.defaultValue(Collections.<String>emptyList())
			.configurationCategory(CORE_PLUGIN_NAME)
			.tags(ELASTICSEARCH)
			.build();
	private final ConfigurationOption<Collection<String>> elasticsearchConfigurationSourceProfiles = ConfigurationOption.stringsOption()
			.key("stagemonitor.elasticsearch.configurationSourceProfiles")
			.dynamic(false)
			.label("Elasticsearch configuration source profiles")
			.description("Set configuration profiles of configuration stored in elasticsearch as a centralized configuration source " +
					"that can be shared between multiple server instances. Set the profiles appropriate to the current " +
					"environment e.g. `production,common`, `local`, `test`, ... The configuration will be stored under " +
					"`{stagemonitor.elasticsearch.url}/stagemonitor/configuration/{configurationSourceProfile}`.")
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
	private final ConfigurationOption<Collection<MetricName>> excludedMetrics = ConfigurationOption
			.builder(new SetValueConverter<MetricName>(new MetricNameValueConverter()), Collection.class)
			.key("stagemonitor.metrics.excluded.pattern")
			.dynamic(false)
			.label("Excluded metric names")
			.description("A comma separated list of metric names that should not be collected.")
			.defaultValue(Collections.<MetricName>emptyList())
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
	private final ConfigurationOption<Collection<String>> excludePackages = ConfigurationOption.stringsOption()
			.key("stagemonitor.instrument.exclude")
			.dynamic(true)
			.label("Excluded packages")
			.description("Exclude packages and their sub-packages from the instrumentation (for example the profiler).")
			.defaultValue(Collections.<String>emptySet())
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Collection<String>> excludeContaining = ConfigurationOption.stringsOption()
			.key("stagemonitor.instrument.excludeContaining")
			.dynamic(true)
			.label("Exclude containing")
			.description("Exclude classes from the instrumentation (for example from profiling) that contain one of the " +
					"following strings as part of their class name.")
			.defaultValue(new LinkedHashSet<String>() {{
				add("$JaxbAccessor");
				add("$$");
				add("CGLIB");
				add("EnhancerBy");
				add("$Proxy");
			}})
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Collection<String>> includePackages = ConfigurationOption.stringsOption()
			.key("stagemonitor.instrument.include")
			.dynamic(true)
			.label("Included packages")
			.description("The packages that should be included for instrumentation (for example the profiler). " +
					"If this property is empty, all packages (except for the excluded ones) are instrumented. " +
					"You can exclude subpackages of a included package via `stagemonitor.instrument.exclude`.")
			.defaultValue(Collections.<String>emptySet())
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Boolean> attachAgentAtRuntime = ConfigurationOption.booleanOption()
			.key("stagemonitor.instrument.runtimeAttach")
			.dynamic(false)
			.label("Attach agent at runtime")
			.description("Attaches the agent via the Attach API at runtime and retransforms all currently loaded classes.")
			.defaultValue(true)
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Collection<String>> exportClassesWithName = ConfigurationOption.stringsOption()
			.key("stagemonitor.instrument.exportGeneratedClassesWithName")
			.dynamic(false)
			.label("Export generated classes with name")
			.description("A list of the fully qualified class names which should be exported to the file system after they have been " +
					"modified by Byte Buddy. This option is useful to debug problems inside the generated class. " +
					"Classes are exported to a temporary directory. The logs contain the information where the files " +
					"are stored.")
			.defaultValue(Collections.<String>emptySet())
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Boolean> debugInstrumentation = ConfigurationOption.booleanOption()
			.key("stagemonitor.instrument.debug")
			.dynamic(false)
			.label("Debug instrumentation")
			.description("Set to true to log additional information and warnings during the instrumentation process.")
			.defaultValue(false)
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Collection<String>> excludedInstrumenters = ConfigurationOption.stringsOption()
			.key("stagemonitor.instrument.excludedInstrumenter")
			.dynamic(false)
			.label("Excluded Instrumenters")
			.description("A list of the simple class names of StagemonitorByteBuddyTransformers that should not be applied")
			.defaultValue(Collections.<String>emptySet())
			.configurationCategory(CORE_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<String> grafanaUrl = ConfigurationOption.stringOption()
			.key("stagemonitor.grafana.url")
			.dynamic(true)
			.label("Grafana URL")
			.description("The URL of your Grafana 2.0 installation")
			.defaultValue(null)
			.configurationCategory(CORE_PLUGIN_NAME)
			.tags("grafana")
			.build();
	private final ConfigurationOption<String> grafanaApiKey = ConfigurationOption.stringOption()
			.key("stagemonitor.grafana.apiKey")
			.dynamic(true)
			.label("Grafana API Key")
			.description("The API Key of your Grafana 2.0 installation. " +
					"See http://docs.grafana.org/reference/http_api/#create-api-token how to create a key. " +
					"The key has to have the admin role. This is necessary so that stagemonitor can automatically add " +
					"datasources and dashboards to Grafana.")
			.defaultValue(null)
			.configurationCategory(CORE_PLUGIN_NAME)
			.tags("grafana")
			.sensitive()
			.build();
	private final ConfigurationOption<Integer> threadPoolQueueCapacityLimit = ConfigurationOption.integerOption()
			.key(POOLS_QUEUE_CAPACITY_LIMIT_KEY)
			.dynamic(false)
			.label("Thread Pool Queue Capacity Limit")
			.description("Sets a limit to the number of pending tasks in the ExecutorServices stagemonitor uses. " +
					"These are thread pools that are used for example to report spans to elasticsearch. " +
					"If elasticsearch is unreachable or your application encounters a spike in incoming requests this limit could be reached. " +
					"It is used to prevent the queue from growing indefinitely. ")
			.defaultValue(1000)
			.configurationCategory(CORE_PLUGIN_NAME)
			.tags("advanced")
			.build();
	private final ConfigurationOption<String> metricsIndexTemplate = ConfigurationOption.stringOption()
			.key("stagemonitor.reporting.elasticsearch.metricsIndexTemplate")
			.dynamic(true)
			.label("ES Metrics Index Template")
			.description("The classpath location of the index template that is used for the stagemonitor-metrics-* indices. " +
					"By specifying the location to your own template, you can fully customize the index template.")
			.defaultValue("stagemonitor-elasticsearch-metrics-index-template.json")
			.configurationCategory(CORE_PLUGIN_NAME)
			.tags(METRICS_STORE, ELASTICSEARCH)
			.build();
	private final ConfigurationOption<Integer> elasticsearchAvailabilityCheckPeriodSec = ConfigurationOption.integerOption()
			.key("stagemonitor.elasticsearch.availabilityCheckPeriodSec")
			.dynamic(false)
			.label("Elasticsearch availability check period (sec)")
			.description("When set to a value > 0 stagemonitor periodically checks if Elasticsearch is available. " +
					"When not available, stagemonitor won't try send documents to Elasticsearch which would " +
					"fail anyway. This reduces heap usage as the documents won't be queued up. " +
					"It also avoids the logging of warnings when the queue is filled up to the limit (see '" + POOLS_QUEUE_CAPACITY_LIMIT_KEY + "')")
			.defaultValue(5)
			.configurationCategory(CORE_PLUGIN_NAME)
			.tags("elasticsearch", "advanced")
			.build();

	private MetricsAggregationReporter aggregationReporter;

	private List<Closeable> reporters = new CopyOnWriteArrayList<Closeable>();

	private ElasticsearchClient elasticsearchClient;
	private GrafanaClient grafanaClient;
	private IndexSelector indexSelector = new IndexSelector(new Clock.UserTimeClock());
	private Metric2Registry metricRegistry;
	private AtomicInteger accessesToElasticsearchUrl = new AtomicInteger();
	private boolean initialized;

	public CorePlugin() {
	}

	public CorePlugin(ElasticsearchClient elasticsearchClient) {
		this.elasticsearchClient = elasticsearchClient;
	}

	@Override
	public void initializePlugin(InitArguments initArguments) {
		this.metricRegistry = initArguments.getMetricRegistry();
		final Integer reloadInterval = getReloadConfigurationInterval();
		if (reloadInterval > 0) {
			initArguments.getConfiguration().scheduleReloadAtRate(reloadInterval, TimeUnit.SECONDS);
		}

		initArguments.getMetricRegistry().register(MetricName.name("online").build(), new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return 1;
			}
		});

		ElasticsearchClient elasticsearchClient = getElasticsearchClient();
		if (isReportToGraphite()) {
			elasticsearchClient.sendGrafana1DashboardAsync("Grafana1GraphiteCustomMetrics.json");
		}
		elasticsearchClient.createIndex("stagemonitor", IOUtils.getResourceAsStream("stagemonitor-elasticsearch-mapping.json"));
		if (isReportToElasticsearch()) {
			final GrafanaClient grafanaClient = getGrafanaClient();
			grafanaClient.createElasticsearchDatasource(getElasticsearchUrl());
			grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchCustomMetricsDashboard.json");
		}
		registerReporters(initArguments.getMetricRegistry(), initArguments.getConfiguration(), initArguments.getMeasurementSession());
		initialized = true;
	}

	void registerReporters(Metric2Registry metric2Registry, Configuration configuration, MeasurementSession measurementSession) {
		Metric2Filter regexFilter = Metric2Filter.ALL;
		Collection<MetricName> excludedMetricsPatterns = getExcludedMetricsPatterns();
		if (!excludedMetricsPatterns.isEmpty()) {
			regexFilter = MetricNameFilter.excludePatterns(excludedMetricsPatterns);
		}

		Metric2Filter allFilters = new AndMetric2Filter(regexFilter, new MetricsWithCountFilter());
		MetricRegistry legacyMetricRegistry = metric2Registry.getMetricRegistry();

		reportToGraphite(legacyMetricRegistry, getGraphiteReportingInterval(), measurementSession);
		reportToInfluxDb(metric2Registry, reportingIntervalInfluxDb.getValue(),
				measurementSession);
		reportToElasticsearch(metric2Registry, reportingIntervalElasticsearch.getValue(), measurementSession);

		List<ScheduledMetrics2Reporter> onShutdownReporters = new LinkedList<ScheduledMetrics2Reporter>();
		onShutdownReporters.add(reportToConsole(metric2Registry, getConsoleReportingInterval(), allFilters));
		registerAggregationReporter(metric2Registry, onShutdownReporters, getAggregationReportingInterval());
		if (configuration.getConfig(CorePlugin.class).isReportToJMX()) {
			// Because JMX reporter is on registration and not periodic only the
			// regex filter is applicable here (not filtering metrics by count)
			reportToJMX(legacyMetricRegistry);
		}
	}

	private void registerAggregationReporter(Metric2Registry metricRegistry,
											 List<ScheduledMetrics2Reporter> onShutdownReporters, long reportingInterval) {
		if (reportingInterval > 0) {
			aggregationReporter = MetricsAggregationReporter.forRegistry(metricRegistry).onShutdownReporters(onShutdownReporters).build();
			aggregationReporter.start(reportingInterval, TimeUnit.SECONDS);
			aggregationReporter.report();
			reporters.add(aggregationReporter);
		}
	}

	private void reportToGraphite(MetricRegistry metricRegistry, long reportingInterval, MeasurementSession measurementSession) {
		if (isReportToGraphite()) {
			final GraphiteReporter graphiteReporter = GraphiteReporter.forRegistry(metricRegistry)
					.prefixedWith(getGraphitePrefix(measurementSession))
					.convertRatesTo(TimeUnit.SECONDS)
					.convertDurationsTo(TimeUnit.MILLISECONDS)
					.build(new Graphite(new InetSocketAddress(getGraphiteHostName(), getGraphitePort())));

			graphiteReporter.start(reportingInterval, TimeUnit.SECONDS);
			reporters.add(graphiteReporter);
		}
	}

	private void reportToInfluxDb(Metric2Registry metricRegistry, int reportingInterval,
								  MeasurementSession measurementSession) {

		if (StringUtils.isNotEmpty(getInfluxDbUrl()) && reportingInterval > 0) {
			logger.info("Sending metrics to InfluxDB ({}) every {}s", getInfluxDbUrl(), reportingInterval);
			final InfluxDbReporter reporter = InfluxDbReporter.forRegistry(metricRegistry, this)
					.globalTags(measurementSession.asMap())
					.build();

			reporter.start(reportingInterval, TimeUnit.SECONDS);
			reporters.add(reporter);
		} else {
			logger.info("Not sending metrics to InfluxDB (url={}, interval={}s)", getInfluxDbUrl(), reportingInterval);
		}
	}

	private void reportToElasticsearch(Metric2Registry metricRegistry, int reportingInterval,
									   final MeasurementSession measurementSession) {
		if (isReportToElasticsearch()) {
			elasticsearchClient.sendClassPathRessourceBulkAsync("stagemonitor-metrics-kibana-index-pattern.bulk");
			logger.info("Sending metrics to Elasticsearch ({}) every {}s", getElasticsearchUrls(), reportingInterval);
			final String mappingJson = ElasticsearchClient.modifyIndexTemplate(
					metricsIndexTemplate.getValue(), moveToColdNodesAfterDays.getValue(), getNumberOfReplicas(), getNumberOfShards());
			elasticsearchClient.sendMappingTemplateAsync(mappingJson, "stagemonitor-metrics");
			elasticsearchClient.scheduleIndexManagement(ElasticsearchReporter.STAGEMONITOR_METRICS_INDEX_PREFIX,
					moveToColdNodesAfterDays.getValue(), deleteElasticsearchMetricsAfterDays.getValue());
		}

		if (isReportToElasticsearch() || isOnlyLogElasticsearchMetricReports()) {
			final ElasticsearchReporter reporter = ElasticsearchReporter.forRegistry(metricRegistry, this)
					.globalTags(measurementSession.asMap())
					.build();

			reporter.start(reportingInterval, TimeUnit.SECONDS);
			reporters.add(reporter);
		} else {
			logger.info("Not sending metrics to Elasticsearch (url={}, interval={}s)", getElasticsearchUrls(), reportingInterval);
		}
	}

	private String getGraphitePrefix(MeasurementSession measurementSession) {
		return name("stagemonitor",
				sanitizeGraphiteMetricSegment(measurementSession.getApplicationName()),
				sanitizeGraphiteMetricSegment(measurementSession.getInstanceName()),
				sanitizeGraphiteMetricSegment(measurementSession.getHostName()));
	}

	private SortedTableLogReporter reportToConsole(Metric2Registry metric2Registry, long reportingInterval, Metric2Filter filter) {
		final SortedTableLogReporter reporter = SortedTableLogReporter.forRegistry(metric2Registry)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS)
				.filter(filter)
				.build();
		if (reportingInterval > 0) {
			reporter.start(reportingInterval, TimeUnit.SECONDS);
			reporters.add(reporter);
		}
		return reporter;
	}

	private void reportToJMX(MetricRegistry metricRegistry) {
		final JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).build();
		reporter.start();
		reporters.add(reporter);
	}

	@Override
	public void onShutDown() {
		if (aggregationReporter != null) {
			logger.info("\n####################################################\n" +
					"## Aggregated report for this measurement session ##\n" +
					"####################################################\n");
			aggregationReporter.onShutDown();
		}

		for (Closeable reporter : reporters) {
			try {
				reporter.close();
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		if (elasticsearchClient != null) {
			elasticsearchClient.close();
		}
		if (grafanaClient != null) {
			grafanaClient.close();
		}
	}

	public MeasurementSession getMeasurementSession() {
		return Stagemonitor.getMeasurementSession();
	}

	public Metric2Registry getMetricRegistry() {
		return metricRegistry;
	}

	public ElasticsearchClient getElasticsearchClient() {
		if (elasticsearchClient == null) {
			elasticsearchClient = new ElasticsearchClient(this, new HttpClient(), elasticsearchAvailabilityCheckPeriodSec.getValue());
		}
		return elasticsearchClient;
	}

	public GrafanaClient getGrafanaClient() {
		if (grafanaClient == null) {
			grafanaClient = new GrafanaClient(this, new HttpClient());
		}
		return grafanaClient;
	}

	public void setElasticsearchClient(ElasticsearchClient elasticsearchClient) {
		this.elasticsearchClient = elasticsearchClient;
	}

	public static String getNameOfLocalHost() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			return getHostNameFromEnv();
		}
	}

	static String getHostNameFromEnv() {
		// try environment properties.
		String host = System.getenv("COMPUTERNAME");
		if (host != null) {
			return host;
		}
		host = System.getenv("HOSTNAME");
		if (host != null) {
			return host;
		}
		return null;
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

	public long getAggregationReportingInterval() {
		return reportingIntervalAggregation.getValue();
	}

	public boolean isReportToJMX() {
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

	public String getHostName() {
		return hostName.getValue();
	}

	/**
	 * Cycles through all provided Elasticsearch URLs and returns one
	 *
	 * @return One of the provided Elasticsearch URLs
	 */
	public String getElasticsearchUrl() {
		final List<String> urls = elasticsearchUrls.getValue();
		if (urls.isEmpty()) {
			return null;
		}
		final int index = accessesToElasticsearchUrl.getAndIncrement() % urls.size();
		return StringUtils.removeTrailingSlash(urls.get(index));
	}

	public Collection<String> getElasticsearchUrls() {
		return elasticsearchUrls.getValue();
	}

	public Collection<String> getElasticsearchConfigurationSourceProfiles() {
		return elasticsearchConfigurationSourceProfiles.getValue();
	}

	public boolean isDeactivateStagemonitorIfEsConfigSourceIsDown() {
		return deactivateStagemonitorIfEsConfigSourceIsDown.getValue();
	}

	public Collection<MetricName> getExcludedMetricsPatterns() {
		return excludedMetrics.getValue();
	}

	public Collection<String> getDisabledPlugins() {
		return disabledPlugins.getValue();
	}

	public Integer getReloadConfigurationInterval() {
		return reloadConfigurationInterval.getValue();
	}

	public Collection<String> getExcludeContaining() {
		return excludeContaining.getValue();
	}

	public Collection<String> getIncludePackages() {
		return includePackages.getValue();
	}

	public Collection<String> getExcludePackages() {
		return excludePackages.getValue();
	}

	public boolean isAttachAgentAtRuntime() {
		return attachAgentAtRuntime.getValue();
	}

	public Collection<String> getExcludedInstrumenters() {
		return excludedInstrumenters.getValue();
	}

	public String getInfluxDbUrl() {
		return StringUtils.removeTrailingSlash(influxDbUrl.getValue());
	}

	public String getInfluxDbDb() {
		return influxDbDb.getValue();
	}

	public boolean isReportToElasticsearch() {
		return !getElasticsearchUrls().isEmpty() && reportingIntervalElasticsearch.getValue() > 0;
	}

	public boolean isReportToGraphite() {
		return StringUtils.isNotEmpty(getGraphiteHostName());
	}

	public String getGrafanaUrl() {
		return StringUtils.removeTrailingSlash(grafanaUrl.getValue());
	}

	public String getGrafanaApiKey() {
		return grafanaApiKey.getValue();
	}

	public int getThreadPoolQueueCapacityLimit() {
		return threadPoolQueueCapacityLimit.getValue();
	}

	public IndexSelector getIndexSelector() {
		return indexSelector;
	}

	public int getElasticsearchReportingInterval() {
		return reportingIntervalElasticsearch.getValue();
	}

	public Integer getMoveToColdNodesAfterDays() {
		return moveToColdNodesAfterDays.getValue();
	}

	public boolean isOnlyLogElasticsearchMetricReports() {
		return onlyLogElasticsearchMetricReports.getValue();
	}

	public boolean isDebugInstrumentation() {
		return debugInstrumentation.getValue();
	}

	public Collection<String> getExportClassesWithName() {
		return exportClassesWithName.getValue();
	}

	public boolean isInitialized() {
		return initialized;
	}

	public Integer getNumberOfReplicas() {
		return numberOfReplicas.getValue();
	}

	public Integer getNumberOfShards() {
		return numberOfShards.getValue();
	}

	List<Closeable> getReporters() {
		return reporters;
	}
}
