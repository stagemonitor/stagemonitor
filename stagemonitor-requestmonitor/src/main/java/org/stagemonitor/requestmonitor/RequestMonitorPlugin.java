package org.stagemonitor.requestmonitor;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.rest.ElasticsearchClient;

public class RequestMonitorPlugin implements StagemonitorPlugin {

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
			.dynamic(false)
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
	private final ConfigurationOption<Long> minExecutionTimeNanos = ConfigurationOption.longOption()
			.key("stagemonitor.profiler.minExecutionTimeNanos")
			.dynamic(false)
			.label("Min execution time (nanos)")
			.description("The minimal inclusive execution time in nanoseconds of a method to be included in a call stack.")
			.defaultValue(100000L)
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
			.defaultValue(true)
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<String> requestTraceTtl = ConfigurationOption.stringOption()
			.key("stagemonitor.requestmonitor.requestTraceTTL")
			.dynamic(true)
			.label("Request trace ttl")
			.description("When set, call stacks will be deleted automatically after the specified interval\n" +
					"In case you do not specify a time unit like d (days), m (minutes), h (hours), " +
					"ms (milliseconds) or w (weeks), milliseconds is used as default unit.")
			.defaultValue("1w")
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

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		return Arrays.<ConfigurationOption<?>>asList(noOfWarmupRequests, warmupSeconds, collectRequestStats, 
				collectCpuTime, minExecutionTimeNanos, callStackEveryXRequestsToGroup, logCallStacks, requestTraceTtl,
				collectDbTimePerRequest);
	}

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration config) {
		addElasticsearchMapping();
		ElasticsearchClient.sendGrafanaDashboardAsync("Request.json");
		ElasticsearchClient.sendKibanaDashboardAsync("Recent Requests.json");
	}

	private void addElasticsearchMapping() {
		InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("stagemonitor-elasticsearch-index-template.json");
		// async, because it is not possible, that request traces are reaching elasticsearch before the mapping is set
		// that is, because a single thread executor is used that executes the request in a linear queue (LinkedBlockingQueue)
		ElasticsearchClient.sendAsJsonAsync("/_template/stagemonitor", "PUT", resourceAsStream);
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

	public String getRequestTraceTtl() {
		return requestTraceTtl.getValue();
	}

	public boolean isCollectDbTimePerRequest() {
		return collectDbTimePerRequest.getValue();
	}
}
