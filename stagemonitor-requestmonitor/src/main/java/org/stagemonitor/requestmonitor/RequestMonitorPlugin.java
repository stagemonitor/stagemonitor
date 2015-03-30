package org.stagemonitor.requestmonitor;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.configuration.source.ConfigurationSource;
import org.stagemonitor.core.configuration.source.PropertyFileConfigurationSource;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

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
			.description("The minimal inclusive execution time in nanoseconds of a method to be included in a call stack. " +
					"This option can only be set in stagemonitor.properties.")
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
	private final ConfigurationOption<Collection<String>> excludePackages = ConfigurationOption.stringsOption()
			.key("stagemonitor.profiler.exclude")
			.dynamic(true)
			.label("Excluded packages")
			.description("Exclude packages and their sub-packages from the profiler. " +
					"This option can only be set in stagemonitor.properties.")
			.defaultValue(new LinkedHashSet<String>() {{
				add("antlr");
				add("aopalliance");
				add("asm");
				add("c3p0");
				add("ch.qos");
				add("com.amazon");
				add("com.codahale");
				add("com.fasterxml");
				add("com.github");
				add("com.google");
				add("com.maxmind");
				add("com.oracle");
				add("com.rome");
				add("com.spartial");
				add("com.sun");
				add("com.thoughtworks");
				add("com.vaadin");
				add("commons-");
				add("dom4j");
				add("eclipse");
				add("java.");
				add("javax.");
				add("junit");
				add("net.java");
				add("net.sf");
				add("net.sourceforge");
				add("org.antlr");
				add("org.apache");
				add("org.aspectj");
				add("org.codehaus");
				add("org.eclipse");
				add("org.freemarker");
				add("org.glassfish");
				add("org.hibernate");
				add("org.hsqldb");
				add("org.jadira");
				add("org.javassist");
				add("org.jboss");
				add("org.jdom");
				add("org.joda");
				add("org.jsoup");
				add("org.json");
				add("org.elasticsearch");
				add("org.slf4j");
				add("org.springframework");
				add("org.stagemonitor");
				add("org.yaml");
				add("org.wildfly");
				add("org.zeroturnaround");
				add("io.dropwizard");
				add("freemarker");
				add("uadetector");
				add("p6spy");
				add("rome");
				add("sun");
				add("xerces");
				add("xml");
				add("xmpp");
			}})
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Collection<String>> excludeContaining = ConfigurationOption.stringsOption()
			.key("stagemonitor.profiler.excludeContaining")
			.dynamic(true)
			.label("Exclude containing")
			.description("Exclude classes from the profiler that contain one of the following strings " +
					"as part of their canonical class name. " +
					"This option can only be set in stagemonitor.properties.")
			.defaultValue(new LinkedHashSet<String>() {{
				add("$JaxbAccessor");
				add("$$");
				add("CGLIB");
			}})
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.build();
	private final ConfigurationOption<Collection<String>> includePackages = ConfigurationOption.stringsOption()
			.key("stagemonitor.profiler.include")
			.dynamic(true)
			.label("Included packages")
			.description("The packages that should be included for profiling. " +
					"This option can only be set in stagemonitor.properties.")
			.defaultValue(Collections.<String>emptySet())
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

	private static RequestMonitor requestMonitor;

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration config) {
		ElasticsearchClient elasticsearchClient = config.getConfig(CorePlugin.class).getElasticsearchClient();
		addElasticsearchMapping(elasticsearchClient);
		elasticsearchClient.sendGrafanaDashboardAsync("Request.json");
		elasticsearchClient.sendKibanaDashboardAsync("Recent Requests.json");
	}

	private void addElasticsearchMapping(ElasticsearchClient elasticsearchClient) {
		InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("stagemonitor-elasticsearch-index-template.json");
		// async, because it is not possible, that request traces are reaching elasticsearch before the mapping is set
		// that is, because a single thread executor is used that executes the request in a linear queue (LinkedBlockingQueue)
		elasticsearchClient.sendAsJsonAsync("PUT", "/_template/stagemonitor", resourceAsStream);
	}

	@Override
	public List<String> getPathsOfWidgetMetricTabPlugins() {
		return Arrays.asList("/stagemonitor/static/tabs/metrics/request-metrics");
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

	public String getRequestTraceTtl() {
		return requestTraceTtl.getValue();
	}

	public boolean isCollectDbTimePerRequest() {
		return collectDbTimePerRequest.getValue();
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

	/**
	 * Returns a instance of RequestMonitorPlugin whose only configuration source is stagemonitor.properties.
	 *
	 * @return a instance of RequestMonitorPlugin whose only configuration source is stagemonitor.properties
	 */
	public static RequestMonitorPlugin getSimpleInstance() {
		return new Configuration(Arrays.asList(new RequestMonitorPlugin()),
				Arrays.<ConfigurationSource>asList(new PropertyFileConfigurationSource("stagemonitor.properties")),
				null).getConfig(RequestMonitorPlugin.class);
	}
}
