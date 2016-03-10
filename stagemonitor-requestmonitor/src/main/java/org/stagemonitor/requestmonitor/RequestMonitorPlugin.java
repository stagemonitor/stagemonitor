package org.stagemonitor.requestmonitor;

import java.util.Collection;
import java.util.Collections;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.grafana.GrafanaClient;
import org.stagemonitor.requestmonitor.reporter.ElasticsearchRequestTraceReporter;

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
	private final ConfigurationOption<Double> onlyCollectNCallTreesPerMinute = ConfigurationOption.doubleOption()
			.key("stagemonitor.requestmonitor.onlyReportNRequestsPerMinuteToElasticsearch")
			.dynamic(true)
			.label("Only report N requests per minute to ES")
			.description("Limits the rate at which call trees are collected. " +
					"Set to a value below 1 to deactivate call tree recording and to 1,000,000 or higher to always collect.")
			.defaultValue(1000000d)
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
			.tags("privacy")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Boolean> pseudonymizeUserName = ConfigurationOption.booleanOption()
			.key("stagemonitor.pseudonymize.username")
			.dynamic(true)
			.label("Pseudonymize Usernames")
			.description("Stagemonitor collects the user names which may be a privacy issue. " +
					"If set to true, the user name will be pseudonymized (SHA1 hashed).")
			.defaultValue(false)
			.tags("privacy")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Collection<String>> discloseUsers = ConfigurationOption.stringsOption()
			.key("stagemonitor.disclose.users")
			.dynamic(true)
			.label("Disclose users")
			.description("When you pseudonymize user names and detect that a specific user seems malicious, " +
					"you can disclose their real user name to make further investigations. Also, the IP won't be " +
					"anonymized anymore for these users. " +
					"If pseudonymizing user names is active you can specify a list of user name pseudonyms to disclose. " +
					"If not, just use the plain user names to disclose their IP address.")
			.defaultValue(Collections.<String>emptySet())
			.tags("privacy")
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
	private final ConfigurationOption<Double> onlyReportNRequestsPerMinuteToElasticsearch = ConfigurationOption.doubleOption()
			.key("stagemonitor.requestmonitor.onlyReportNRequestsPerMinuteToElasticsearch")
			.dynamic(true)
			.label("Only report N requests per minute to ES")
			.description("Limits the rate at which request traces are reported to Elasticsearch. " +
					"Set to a value below 1 to deactivate ES reporting and to 1,000,000 or higher to always report.")
			.defaultValue(1000000d)
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Boolean> onlyLogElasticsearchRequestTraceReports = ConfigurationOption.booleanOption()
			.key("stagemonitor.requestmonitor.elasticsearch.onlyLogElasticsearchRequestTraceReports")
			.dynamic(false)
			.label("Only log Elasticsearch request trace reports")
			.description(String.format("If set to true, the request traces won't be reported to elasticsearch but instead logged in bulk format. " +
					"The name of the logger is %s. That way you can redirect the reporting to a separate log file and use logstash or a " +
					"different external process to send the request traces to elasticsearch.", ElasticsearchRequestTraceReporter.ES_REQUEST_TRACE_LOGGER))
			.defaultValue(false)
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Double> excludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests = ConfigurationOption.doubleOption()
			.key("stagemonitor.requestmonitor.elasticsearch.excludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests")
			.dynamic(true)
			.label("Exclude the Call Tree from Elasticsearch reports on x% of the fastest requests")
			.description("Exclude the Call Tree from Elasticsearch report when the request was faster faster than x " +
					"percent of requests with the same request name. This helps to reduce the network and disk overhead " +
					"as uninteresting Call Trees (those which are comparatively fast) are excluded." +
					"Example: set to 1 to always exclude the Call Tree and to 0 to always include it. " +
					"With a setting of 0.85, the Call Tree will only be reported for the slowest 25% of the requests.")
			.defaultValue(0d)
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Collection<String>> unnestExceptions = ConfigurationOption.stringsOption()
			.key("stagemonitor.requestmonitor.unnestExeptions")
			.dynamic(true)
			.label("Unnest Exceptions")
			.description("Some Exceptions are so called 'nested exceptions' which wrap the actual cause of the exception. " +
					"A prominent example is Spring's NestedServletException. " +
					"In those cases it makes sense to unnest the exception to see the actual cause in the request analysis dashboard.")
			.defaultValue(Collections.singleton("org.springframework.web.util.NestedServletException"))
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();

	private static RequestMonitor requestMonitor;

	@Override
	public void initializePlugin(StagemonitorPlugin.InitArguments initArguments) {
		final CorePlugin corePlugin = initArguments.getPlugin(CorePlugin.class);
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
			elasticsearchClient.sendBulkAsync("kibana/StagemonitorRequestsIndexPattern.bulk");
			elasticsearchClient.sendBulkAsync("kibana/RequestDashboard.bulk");
			elasticsearchClient.sendBulkAsync("kibana/RequestAnalysis.bulk");
			elasticsearchClient.sendBulkAsync("kibana/WebAnalytics.bulk");
			grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchRequestDashboard.json");
			elasticsearchClient.scheduleIndexManagement("stagemonitor-requests-",
					corePlugin.getMoveToColdNodesAfterDays(), deleteRequestTracesAfterDays.getValue());
		}
	}

	@Override
	public void registerWidgetMetricTabPlugins(WidgetMetricTabPluginsRegistry widgetMetricTabPluginsRegistry) {
		widgetMetricTabPluginsRegistry.addWidgetMetricTabPlugin("/stagemonitor/static/tabs/metrics/request-metrics");
	}

	public RequestMonitor getRequestMonitor() {
		if (requestMonitor == null) {
			requestMonitor = new RequestMonitor(Stagemonitor.getConfiguration(), Stagemonitor.getMetric2Registry());
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

	public double getOnlyCollectNCallTreesPerMinute() {
		return onlyCollectNCallTreesPerMinute.getValue();
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

	public boolean isPseudonymizeUserNames() {
		return pseudonymizeUserName.getValue();
	}

	public Collection<String> getDiscloseUsers() {
		return discloseUsers.getValue();
	}

	public Collection<String> getOnlyReportRequestsWithNameToElasticsearch() {
		return onlyReportRequestsWithNameToElasticsearch.getValue();
	}

	public double getOnlyReportNRequestsPerMinuteToElasticsearch() {
		return onlyReportNRequestsPerMinuteToElasticsearch.getValue();
	}

	public boolean isOnlyLogElasticsearchRequestTraceReports() {
		return onlyLogElasticsearchRequestTraceReports.getValue();
	}

	public double getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests() {
		return excludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests.getValue();
	}

	public Collection<String> getUnnestExceptions() {
		return unnestExceptions.getValue();
	}
}
