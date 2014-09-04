package org.stagemonitor.requestmonitor;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.ConfigurationOption;
import org.stagemonitor.core.StageMonitorPlugin;
import org.stagemonitor.core.rest.RestClient;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RequestMonitorPlugin implements StageMonitorPlugin {

	public static final String NO_OF_WARMUP_REQUESTS = "stagemonitor.requestmonitor.noOfWarmupRequests";
	public static final String WARMUP_SECONDS = "stagemonitor.requestmonitor.warmupSeconds";
	public static final String COLLECT_REQUEST_STATS = "stagemonitor.requestmonitor.collectRequestStats";
	public static final String CPU_TIME = "stagemonitor.requestmonitor.cpuTime";
	public static final String PROFILER_MIN_EXECUTION_TIME_NANOS = "stagemonitor.profiler.minExecutionTimeNanos";
	public static final String CALL_STACK_EVERY_XREQUESTS_TO_GROUP = "stagemonitor.profiler.callStackEveryXRequestsToGroup";
	public static final String LOG_CALL_STACKS = "stagemonitor.profiler.logCallStacks";
	public static final String REQUEST_TRACE_TTL = "stagemonitor.requestmonitor.requestTraceTTL";
	public static final String COLLECT_DB_TIME_PER_REQUEST = "stagemonitor.jdbc.collectDbTimePerRequest";

	@Override
	public List<ConfigurationOption> getConfigurationOptions() {
		List<ConfigurationOption> config = new ArrayList<ConfigurationOption>();
		config.add(ConfigurationOption.builder()
				.key(NO_OF_WARMUP_REQUESTS)
				.dynamic(false)
				.label("Number of warmup requests")
				.description("the minimum number of requests that have to be issued against the application before metrics are collected")
				.defaultValue("0")
				.build());
		config.add(ConfigurationOption.builder()
				.key(WARMUP_SECONDS)
				.dynamic(false)
				.label("Number of warmup seconds")
				.description("A timespan in seconds after the start of the server where no metrics are collected.")
				.defaultValue("0")
				.build());
		config.add(ConfigurationOption.builder()
				.key(COLLECT_REQUEST_STATS)
				.dynamic(false)
				.label("Collect request stats")
				.description("Whether or not metrics about requests (Call Stacks, response times, errors status codes) should be collected.")
				.defaultValue("true")
				.build());
		config.add(ConfigurationOption.builder()
				.key(CPU_TIME)
				.dynamic(true)
				.label("Collect CPU time")
				.description("Whether or not a timer for the cpu time of executions should be created.")
				.defaultValue("false")
				.build());
		config.add(ConfigurationOption.builder()
				.key(PROFILER_MIN_EXECUTION_TIME_NANOS)
				.dynamic(false)
				.label("Min execution time (nanos)")
				.description("The minimal inclusive execution time in nanoseconds of a method to be included in a call stack.")
				.defaultValue("100000")
				.build());
		config.add(ConfigurationOption.builder()
				.key(CALL_STACK_EVERY_XREQUESTS_TO_GROUP)
				.dynamic(true)
				.label("Gather call tree every x requests to URL group")
				.description("Defines after how many requests to a URL group a call tree should be collected.")
				.defaultValue("1")
				.build());
		config.add(ConfigurationOption.builder()
				.key(LOG_CALL_STACKS)
				.dynamic(true)
				.label("Log call tree")
				.description("Whether or not call stacks should be logged.")
				.defaultValue("true")
				.build());
		config.add(ConfigurationOption.builder()
				.key(REQUEST_TRACE_TTL)
				.dynamic(true)
				.label("Request trace ttl")
				.description("When set, call stacks will be deleted automatically after the specified interval\n" +
						"In case you do not specify a time unit like d (days), m (minutes), h (hours), " +
						"ms (milliseconds) or w (weeks), milliseconds is used as default unit.")
				.defaultValue("1w")
				.build());
		return config;
	}

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration config) {
		addElasticsearchMapping(config.getElasticsearchUrl());
		RestClient.sendGrafanaDashboardAsync(config.getElasticsearchUrl(), "Request.json");
		RestClient.sendKibanaDashboardAsync(config.getElasticsearchUrl(), "Recent Requests.json");
	}

	private void addElasticsearchMapping(String serverUrl) {
		InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("stagemonitor-elasticsearch-index-template.json");
		// async, because it is not possible, that request traces are reaching elasticsearch before the mapping is set
		// that is, because a single thread executor is used that executes the request in a linear queue (LinkedBlockingQueue)
		RestClient.sendAsJsonAsync(serverUrl, "/_template/stagemonitor", "PUT", resourceAsStream);
	}

}
