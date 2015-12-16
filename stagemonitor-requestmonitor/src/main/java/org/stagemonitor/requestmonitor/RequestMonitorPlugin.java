package org.stagemonitor.requestmonitor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.grafana.GrafanaClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

public class RequestMonitorPlugin extends StagemonitorPlugin {

	public static final String REQUEST_MONITOR_PLUGIN = "Request Monitor Plugin";
	private final ConfigurationOption<Integer> noOfWarmupRequests = ConfigurationOption.integerOption()
			.key("stagemonitor.requestmonitor.noOfWarmupRequests")
			.dynamic(false)
			.label("Number of warmup requests")
			.description("the minimum number of requests that have to be issued against the application before metrics are collected")
			.defaultValue(0)
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Integer> warmupSeconds = ConfigurationOption.integerOption()
			.key("stagemonitor.requestmonitor.warmupSeconds")
			.dynamic(false)
			.label("Number of warmup seconds")
			.description("A timespan in seconds after the start of the server where no metrics are collected.")
			.defaultValue(0)
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Boolean> collectRequestStats = ConfigurationOption.booleanOption()
			.key("stagemonitor.requestmonitor.collectRequestStats")
			.dynamic(true)
			.label("Collect request stats")
			.description("Whether or not metrics about requests (Call Stacks, response times, errors status codes) should be collected.")
			.defaultValue(true)
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Boolean> collectCpuTime = ConfigurationOption.booleanOption()
			.key("stagemonitor.requestmonitor.cpuTime")
			.dynamic(true)
			.label("Collect CPU time")
			.description("Whether or not a timer for the cpu time of executions should be created.")
			.defaultValue(false)
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Boolean> profilerActive = ConfigurationOption.booleanOption()
			.key("stagemonitor.profiler.active")
			.dynamic(false)
			.label("Activate Profiler")
			.description("Whether or not the call tree profiler should be active.")
			.defaultValue(true)
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Long> minExecutionTimeNanos = ConfigurationOption.longOption()
			.key("stagemonitor.profiler.minExecutionTimeNanos")
			.dynamic(false)
			.label("Min execution time (nanos)")
			.description("Don't show methods that executed faster than this value in the call tree (1 ms = 1,000,000 ns).")
			.defaultValue(100000L)
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Double> minExecutionTimePercent = ConfigurationOption.doubleOption()
			.key("stagemonitor.profiler.minExecutionTimePercent")
			.dynamic(true)
			.label("Min execution time (%)")
			.description("Don't show methods that executed faster than this value in the call tree (0.5 or 0,5 means 0.5%).")
			.defaultValue(0.5)
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Integer> callStackEveryXRequestsToGroup = ConfigurationOption.integerOption()
			.key("stagemonitor.profiler.callStackEveryXRequestsToGroup")
			.dynamic(true)
			.label("Gather call tree every x requests to URL group")
			.description("Defines after how many requests to a URL group a call tree should be collected.")
			.defaultValue(1)
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Boolean> logCallStacks = ConfigurationOption.booleanOption()
			.key("stagemonitor.profiler.logCallStacks")
			.dynamic(true)
			.label("Log call tree")
			.description("Whether or not call stacks should be logged.")
			.defaultValue(false)
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Integer> deleteRequestTracesAfterDays = ConfigurationOption.integerOption()
			.key("stagemonitor.requestmonitor.deleteRequestTracesAfterDays")
			.dynamic(true)
			.label("Delete request traces after (days)")
			.description("When set, call stacks will be deleted automatically after the specified days. " +
					"Set to a negative value to never delete request traces.")
			.defaultValue(7)
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Boolean> collectDbTimePerRequest = ConfigurationOption.booleanOption()
			.key("stagemonitor.jdbc.collectDbTimePerRequest")
			.dynamic(true)
			.label("Collect db time per request group")
			.description("Whether or not db execution time should be collected per request group\n" +
					"If set to true, a timer will be created for each request to record the total db time per request.")
			.defaultValue(false)
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<BusinessTransactionNamingStrategy> businessTransactionNamingStrategy = ConfigurationOption.enumOption(BusinessTransactionNamingStrategy.class)
			.key("stagemonitor.businessTransaction.namingStrategy")
			.dynamic(false)
			.label("Business Transaction naming strategy")
			.description("Defines how to name a business transaction that was detected by a method call. " +
					"For example a Spring-MVC controller method or a method that is annotated with @"+MonitorRequests.class.getSimpleName()+". " +
					BusinessTransactionNamingStrategy.METHOD_NAME_SPLIT_CAMEL_CASE + ": Say Hello " +
					BusinessTransactionNamingStrategy.CLASS_NAME_DOT_METHOD_NAME + ": HelloController.sayHello " +
					BusinessTransactionNamingStrategy.CLASS_NAME_HASH_METHOD_NAME + ": HelloController#sayHello ")
			.defaultValue(BusinessTransactionNamingStrategy.METHOD_NAME_SPLIT_CAMEL_CASE)
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Boolean> anonymizeIPs = ConfigurationOption.booleanOption()
			.key("stagemonitor.anonymizeIPs")
			.dynamic(true)
			.label("Anonymize IP Addresses")
			.description("For IPv4 addresses, the last octet is set to zero. " +
					"If the address is a IPv6 address, the last 80 bits (10 bytes) are set to zero. " +
					"This is just like Google Analytics handles IP anonymization.")
			.defaultValue(true)
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Boolean> reportRequestTracesToElasticsearch = ConfigurationOption.booleanOption()
			.key("stagemonitor.requestmonitor.reportRequestTracesToElasticsearch")
			.dynamic(true)
			.label("Report request traces to Elasticsearch")
			.description("Whether request traces should be persisted to Elasticsearch. If you want to enable this, make sure to also set the Elasticsearch url.")
			.defaultValue(true)
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Collection<String>> onlyReportRequestsWithNameToElasticsearch = ConfigurationOption.stringsOption()
			.key("stagemonitor.requestmonitor.onlyReportRequestsWithNameToElasticsearch")
			.dynamic(true)
			.label("Only report request traces with name to ES")
			.description("Limits the reporting of request traces to Elasticsearch to requests with a certain name.")
			.defaultValue(Collections.<String>emptySet())
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();

	private static RequestMonitor requestMonitor;

	@Override
	public void initializePlugin(Metric2Registry metricRegistry, Configuration config) {
		final CorePlugin corePlugin = config.getConfig(CorePlugin.class);
		final ElasticsearchClient elasticsearchClient = corePlugin.getElasticsearchClient();
		final GrafanaClient grafanaClient = corePlugin.getGrafanaClient();
		final String mappingJson = ElasticsearchClient.requireBoxTypeHotIfHotColdAritectureActive(
				"stagemonitor-elasticsearch-request-index-template.json", corePlugin.getMoveToColdNodesAfterDays());
		elasticsearchClient.sendMappingTemplateAsync(mappingJson, "stagemonitor-requests");
		elasticsearchClient.sendKibanaDashboardAsync("kibana/Kibana3RecentRequests.json");
		if (corePlugin.isReportToGraphite()) {
			elasticsearchClient.sendGrafana1DashboardAsync("grafana/Grafana1GraphiteRequestDashboard.json");
		}
		if (corePlugin.isReportToElasticsearch()) {
			elasticsearchClient.sendBulkAsync("kibana/RequestDashboard.bulk");
			elasticsearchClient.sendBulkAsync("kibana/RequestAnalysis.bulk");
			grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchRequestDashboard.json");
			elasticsearchClient.scheduleIndexManagement("stagemonitor-requests-",
					corePlugin.getMoveToColdNodesAfterDays(), deleteRequestTracesAfterDays.getValue());
		}
	}

	@Override
	public List<String> getPathsOfWidgetMetricTabPlugins() {
		return Collections.singletonList("/stagemonitor/static/tabs/metrics/request-metrics");
	}

	public RequestMonitor getRequestMonitor() {
		if (requestMonitor == null) {
			requestMonitor = new RequestMonitor();
		}
		return requestMonitor;
	}

	public int getNoOfWarmupRequests() {
		return noOfWarmupRequests.getValue();
	}

	public int getWarmupSeconds() {
		return warmupSeconds.getValue();
	}

	public boolean isCollectRequestStats() {
		return collectRequestStats.getValue();
	}

	public boolean isCollectCpuTime() {
		return collectCpuTime.getValue();
	}

	public long getMinExecutionTimeNanos() {
		return minExecutionTimeNanos.getValue();
	}

	public int getCallStackEveryXRequestsToGroup() {
		return callStackEveryXRequestsToGroup.getValue();
	}

	public boolean isLogCallStacks() {
		return logCallStacks.getValue();
	}

	public boolean isCollectDbTimePerRequest() {
		return collectDbTimePerRequest.getValue();
	}

	public boolean isProfilerActive() {
		return profilerActive.getValue();
	}

	public BusinessTransactionNamingStrategy getBusinessTransactionNamingStrategy() {
		return businessTransactionNamingStrategy.getValue();
	}

	@Override
	public void onShutDown() {
		getRequestMonitor().close();
	}

	public double getMinExecutionTimePercent() {
		return minExecutionTimePercent.getValue();
	}

	public boolean isAnonymizeIPs() {
		return anonymizeIPs.getValue();
	}

	public Collection<String> getOnlyReportRequestsWithNameToElasticsearch() {
		return onlyReportRequestsWithNameToElasticsearch.getValue();
	}

	public boolean isReportRequestTracesToElasticsearch() {
		return reportRequestTracesToElasticsearch.getValue();
	}
}
