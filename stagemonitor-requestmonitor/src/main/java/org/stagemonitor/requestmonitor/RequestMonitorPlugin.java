package org.stagemonitor.requestmonitor;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.grafana.GrafanaClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.requestmonitor.anonymization.AnonymizingSpanEventListener;
import org.stagemonitor.requestmonitor.metrics.ExternalRequestMetricsSpanEventListener;
import org.stagemonitor.requestmonitor.metrics.ServerRequestMetricsSpanEventListener;
import org.stagemonitor.requestmonitor.reporter.ElasticsearchSpanReporter;
import org.stagemonitor.requestmonitor.sampling.PostExecutionSpanInterceptor;
import org.stagemonitor.requestmonitor.sampling.PreExecutionSpanInterceptor;
import org.stagemonitor.requestmonitor.sampling.SamplePriorityDeterminingSpanEventListener;
import org.stagemonitor.requestmonitor.tracing.TracerFactory;
import org.stagemonitor.requestmonitor.tracing.jaeger.MDCSpanEventListener;
import org.stagemonitor.requestmonitor.tracing.jaeger.SpanJsonModule;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanEventListenerFactory;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrappingTracer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Pattern;

import io.opentracing.Tracer;

public class RequestMonitorPlugin extends StagemonitorPlugin {

	public static final String REQUEST_MONITOR_PLUGIN = "Request Monitor Plugin";

	/* What/how to monitor */
	private final ConfigurationOption<Boolean> collectCpuTime = ConfigurationOption.booleanOption()
			.key("stagemonitor.requestmonitor.cpuTime")
			.dynamic(true)
			.label("Collect CPU time")
			.description("Whether or not a timer for the cpu time of executions should be created. " +
					"This is useful if you want to know which use cases are responsible for the most CPU usage. " +
					"Be aware that setting this to true almost doubles the amount of timers created.")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(false);
	private final ConfigurationOption<Boolean> collectDbTimePerRequest = ConfigurationOption.booleanOption()
			.key("stagemonitor.requestmonitor.collectExternalRequestTimePerRequest")
			.dynamic(true)
			.label("Collect external request time per request group")
			.description("Whether or not the execution time of external should be collected per request group\n" +
					"If set to true, a timer will be created for each request to record the total db time per request.")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(false);
	private final ConfigurationOption<BusinessTransactionNamingStrategy> businessTransactionNamingStrategy = ConfigurationOption.enumOption(BusinessTransactionNamingStrategy.class)
			.key("stagemonitor.businessTransaction.namingStrategy")
			.dynamic(false)
			.label("Business Transaction naming strategy")
			.description("Defines how to name a business transaction that was detected by a method call. " +
					"For example a Spring-MVC controller method or a method that is annotated with @" + MonitorRequests.class.getSimpleName() + ". " +
					BusinessTransactionNamingStrategy.METHOD_NAME_SPLIT_CAMEL_CASE + ": Say Hello " +
					BusinessTransactionNamingStrategy.CLASS_NAME_DOT_METHOD_NAME + ": HelloController.sayHello " +
					BusinessTransactionNamingStrategy.CLASS_NAME_HASH_METHOD_NAME + ": HelloController#sayHello ")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(BusinessTransactionNamingStrategy.METHOD_NAME_SPLIT_CAMEL_CASE);
	private final ConfigurationOption<Boolean> monitorScheduledTasks = ConfigurationOption.booleanOption()
			.key("stagemonitor.requestmonitor.monitorScheduledTasks")
			.dynamic(false)
			.label("Monitor scheduled tasks")
			.description("Set to true trace EJB (@Schedule) and Spring (@Scheduled) scheduled tasks.")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(false);
	private final ConfigurationOption<Collection<Pattern>> confidentialParameters = ConfigurationOption.regexListOption()
			.key("stagemonitor.requestmonitor.params.confidential.regex")
			.dynamic(true)
			.label("Confidential parameters (regex)")
			.description("A list of request parameter name patterns that should not be collected.\n" +
					"In the context of a HTTP request, a request parameter is either a query string or a application/x-www-form-urlencoded request " +
					"body (POST form content). In the context of a method invocation monitored with @MonitorRequests," +
					"this refers to the parameter name of the monitored method. Note that you have to compile your classes" +
					"with 'vars' debug information.")
			.tags("security-relevant")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(Arrays.asList(
					Pattern.compile("(?i).*pass.*"),
					Pattern.compile("(?i).*credit.*"),
					Pattern.compile("(?i).*pwd.*"),
					Pattern.compile("(?i)pw")));

	/* Profiler */
	private final ConfigurationOption<Boolean> profilerActive = ConfigurationOption.booleanOption()
			.key("stagemonitor.profiler.active")
			.dynamic(false)
			.label("Activate Profiler")
			.description("Whether or not the call tree profiler should be active.")
			.tags("profiler")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(true);
	private final ConfigurationOption<Long> minExecutionTimeNanos = ConfigurationOption.longOption()
			.key("stagemonitor.profiler.minExecutionTimeNanos")
			.dynamic(false)
			.label("Min execution time (nanos)")
			.description("Don't show methods that executed faster than this value in the call tree (1 ms = 1,000,000 ns).")
			.tags("profiler")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(100000L);
	private final ConfigurationOption<Double> minExecutionTimePercent = ConfigurationOption.doubleOption()
			.key("stagemonitor.profiler.minExecutionTimePercent")
			.dynamic(true)
			.label("Min execution time (%)")
			.description("Don't show methods that executed faster than this value in the call tree (0.5 or 0,5 means 0.5%).")
			.tags("profiler")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(0.5);
	private final ConfigurationOption<Boolean> profilerObjectPooling = ConfigurationOption.booleanOption()
			.key("stagemonitor.profiler.objectPooling")
			.dynamic(false)
			.label("Activate Profiler Object Pooling")
			.description("Activates the experimental object pooling feature for the profiler. When enabled, instances of " +
					"CallStackElement are not garbage collected but put into an object pool when not needed anymore. " +
					"When we need a new instance of CallStackElement, it is not created with `new CallStackElement()` " +
					"but taken from the pool instead. This aims to reduce heap usage and garbage collections caused by " +
					"stagemonitor.")
			.tags("profiler", "experimental")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(false);
	private final ConfigurationOption<Double> onlyCollectNCallTreesPerMinute = ConfigurationOption.doubleOption()
			.key("stagemonitor.requestmonitor.onlyCollectNCallTreesPerMinute")
			.dynamic(true)
			.label("Only report N call trees per minute")
			.description("Limits the rate at which call trees are collected. " +
					"Set to a value below 1 to deactivate call tree recording and to 1000000 or higher to always collect.")
			.tags("profiler", "sampling")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(1000000d);

	/* Privacy */
	private final ConfigurationOption<Boolean> anonymizeIPs = ConfigurationOption.booleanOption()
			.key("stagemonitor.anonymizeIPs")
			.dynamic(true)
			.label("Anonymize IP Addresses")
			.description("For IPv4 addresses, the last octet is set to zero. " +
					"If the address is a IPv6 address, the last 80 bits (10 bytes) are set to zero. " +
					"This is just like Google Analytics handles IP anonymization.")
			.tags("privacy")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(true);
	private final ConfigurationOption<Boolean> pseudonymizeUserName = ConfigurationOption.booleanOption()
			.key("stagemonitor.pseudonymize.username")
			.dynamic(true)
			.label("Pseudonymize Usernames")
			.description("Stagemonitor collects the user names which may be a privacy issue. " +
					"If set to true, the user name will be pseudonymized (SHA1 hashed).")
			.tags("privacy")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(false);
	private final ConfigurationOption<Collection<String>> discloseUsers = ConfigurationOption.stringsOption()
			.key("stagemonitor.disclose.users")
			.dynamic(true)
			.label("Disclose users")
			.description("When you pseudonymize user names and detect that a specific user seems malicious, " +
					"you can disclose their real user name to make further investigations. Also, the IP won't be " +
					"anonymized anymore for these users. " +
					"If pseudonymizing user names is active you can specify a list of user name pseudonyms to disclose. " +
					"If not, just use the plain user names to disclose their IP address.")
			.tags("privacy")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(Collections.<String>emptySet());

	/* Reporting */
	private final ConfigurationOption<Boolean> onlyLogElasticsearchSpanReports = ConfigurationOption.booleanOption()
			.key("stagemonitor.requestmonitor.elasticsearch.onlyLogElasticsearchRequestTraceReports")
			.dynamic(true)
			.label("Only log Elasticsearch request trace reports")
			.description(String.format("If set to true, the spans won't be reported to elasticsearch but instead logged in bulk format. " +
					"The name of the logger is %s. That way you can redirect the reporting to a separate log file and use logstash or a " +
					"different external process to send the spans to elasticsearch.", ElasticsearchSpanReporter.ES_SPAN_LOGGER))
			.tags("reporting")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(false);
	private final ConfigurationOption<Boolean> logSpans = ConfigurationOption.booleanOption()
			.key("stagemonitor.requestmonitor.reporting.log")
			.dynamic(true)
			.label("Log spans")
			.description("Whether or not spans should be logged.")
			.tags("reporting")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(false);
	private final ConfigurationOption<Boolean> reportSpansAsync = ConfigurationOption.booleanOption()
			.key("stagemonitor.requestmonitor.report.async")
			.dynamic(true)
			.label("Report Async")
			.description("Set to true to report collected spans asynchronously. It's recommended to always set this to " +
					"true. Otherwise the performance of your requests will suffer as spans are reported in band.")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.tags("reporting")
			.buildWithDefault(true);

	/* Exceptions */
	private final ConfigurationOption<Collection<String>> unnestExceptions = ConfigurationOption.stringsOption()
			.key("stagemonitor.requestmonitor.unnestExeptions")
			.dynamic(true)
			.label("Unnest Exceptions")
			.description("Some Exceptions are so called 'nested exceptions' which wrap the actual cause of the exception. " +
					"A prominent example is Spring's NestedServletException. " +
					"In those cases it makes sense to unnest the exception to see the actual cause in the request analysis dashboard.")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(Collections.singleton("org.springframework.web.util.NestedServletException"));
	private final ConfigurationOption<Collection<String>> ignoreExceptions = ConfigurationOption.stringsOption()
			.key("stagemonitor.requestmonitor.ignoreExeptions")
			.dynamic(true)
			.label("Ignore Exceptions")
			.description("The class names of exception to ignore. These exceptions won't show up in the span " +
					"and won't cause the error flag of the span to be set to true.")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(Collections.<String>emptyList());

	/* Storage */
	private final ConfigurationOption<String> spanIndexTemplate = ConfigurationOption.stringOption()
			.key("stagemonitor.requestmonitor.elasticsearch.spanIndexTemplate")
			.dynamic(false)
			.label("ES Request Span Template")
			.description("The classpath location of the index template that is used for the stagemonitor-spans-* indices. " +
					"By specifying the location to your own template, you can fully customize the index template.")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.tags("elasticsearch")
			.buildWithDefault("stagemonitor-elasticsearch-span-index-template.json");
	private final ConfigurationOption<Integer> deleteSpansAfterDays = ConfigurationOption.integerOption()
			.key("stagemonitor.requestmonitor.deleteRequestTracesAfterDays")
			.dynamic(true)
			.label("Delete spans after (days)")
			.description("When set, spans will be deleted automatically after the specified days. " +
					"Set to a negative value to never delete spans.")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(7);

	/* Sampling */
	private final ConfigurationOption<Collection<String>> onlyReportSpansWithName = ConfigurationOption.stringsOption()
			.key("stagemonitor.requestmonitor.sampling.onlyReportSpansWithName")
			.aliasKeys("stagemonitor.requestmonitor.onlyReportRequestsWithNameToElasticsearch")
			.dynamic(true)
			.label("Only report these operation names")
			.description("Limits the reporting of spans to operations with a certain name.")
			.tags("sampling")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(Collections.<String>emptySet());
	private final ConfigurationOption<Double> rateLimitServerSpansPerMinute = ConfigurationOption.doubleOption()
			.key("stagemonitor.requestmonitor.sampling.server.rateLimitPerMinute")
			.aliasKeys("stagemonitor.requestmonitor.onlyReportNRequestsPerMinuteToElasticsearch")
			.dynamic(true)
			.label("Rate limit for server spans per minute")
			.description("Limits the rate at which spans are collected reported. " +
					"Set to a value below 1 to deactivate reporting and to 1000000 or higher to always report.")
			.tags("sampling")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(1000000d);
	private final ConfigurationOption<Double> rateLimitClientSpansPerMinute = ConfigurationOption.doubleOption()
			.key("stagemonitor.requestmonitor.sampling.client.rateLimitPerMinute")
			.aliasKeys("stagemonitor.requestmonitor.external.onlyReportNExternalRequestsPerMinute")
			.dynamic(true)
			.label("Rate limit for external requests (client spans) per minute")
			.description("Limits the rate at which external spans are collected and reported. " +
					"Set to a value below 1 to deactivate reporting and to 1000000 or higher to always report.")
			.tags("external-requests", "sampling")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(0d);
	private final ConfigurationOption<Double> excludeCallTreeFromReportWhenFasterThanXPercentOfRequests = ConfigurationOption.doubleOption()
			.key("stagemonitor.requestmonitor.sampling.excludeCallTreeFromReportWhenFasterThanXPercentOfRequests")
			.aliasKeys("stagemonitor.requestmonitor.elasticsearch.excludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests")
			.dynamic(true)
			.label("Exclude the Call Tree from reports on x% of the fastest requests")
			.description("Exclude the Call Tree from report when the request was faster faster than x " +
					"percent of requests with the same request name. This helps to reduce the network and disk overhead " +
					"as uninteresting Call Trees (those which are comparatively fast) are excluded. " +
					"Example: set to 1 to always exclude the Call Tree and to 0 to always include it. " +
					"With a setting of 0.85, the Call Tree will only be reported for the slowest 25% of the requests.")
			.tags("sampling")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(0d);
	private final ConfigurationOption<Double> excludeExternalRequestsWhenFasterThanXPercent = ConfigurationOption.doubleOption()
			.key("stagemonitor.requestmonitor.external.excludeExternalRequestsWhenFasterThanXPercent")
			.dynamic(true)
			.label("Exclude external requests from reporting on x% of the fastest external requests")
			.description("Exclude the external request from reporting when the request was faster faster than x " +
					"percent of external requests with the same initiator (executedBy). This helps to reduce the network and disk overhead " +
					"as uninteresting external requests (those which are comparatively fast) are excluded." +
					"Example: set to 1 to always exclude the external request and to 0 to always include it. " +
					"With a setting of 0.85, the external request will only be reported for the slowest 25% of the requests.")
			.tags("external-requests", "sampling")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(0d);
	private final ConfigurationOption<Double> excludeExternalRequestsFasterThan = ConfigurationOption.doubleOption()
			.key("stagemonitor.requestmonitor.external.excludeExternalRequestsFasterThan")
			.dynamic(true)
			.label("Exclude external requests from reporting when faster than x ms")
			.description("Exclude the external request from reporting when the request was faster faster than x ms.")
			.tags("external-requests", "sampling")
			.configurationCategory(REQUEST_MONITOR_PLUGIN)
			.buildWithDefault(0d);

	private static RequestMonitor requestMonitor;

	private SpanWrappingTracer tracer;
	private SamplePriorityDeterminingSpanEventListener samplePriorityDeterminingSpanInterceptor;

	public Tracer getTracer() {
		return tracer;
	}

	@Override
	public void initializePlugin(final StagemonitorPlugin.InitArguments initArguments) {
		JsonUtils.getMapper().registerModule(new SpanJsonModule());

		final CorePlugin corePlugin = initArguments.getPlugin(CorePlugin.class);
		final ElasticsearchClient elasticsearchClient = corePlugin.getElasticsearchClient();
		final GrafanaClient grafanaClient = corePlugin.getGrafanaClient();

		final String spanMappingJson = ElasticsearchClient.modifyIndexTemplate(
				spanIndexTemplate.getValue(), corePlugin.getMoveToColdNodesAfterDays(), corePlugin.getNumberOfReplicas(), corePlugin.getNumberOfShards());
		elasticsearchClient.sendMappingTemplateAsync(spanMappingJson, "stagemonitor-spans");

		if (corePlugin.isReportToGraphite()) {
			elasticsearchClient.sendGrafana1DashboardAsync("grafana/Grafana1GraphiteRequestDashboard.json");
		}
		if (corePlugin.isReportToElasticsearch()) {
			elasticsearchClient.sendClassPathRessourceBulkAsync("kibana/Request-Metrics.bulk");
			grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchRequestDashboard.json");
			grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchExternalRequestsDashboard.json");
		}
		if (!corePlugin.getElasticsearchUrls().isEmpty()) {
			elasticsearchClient.sendClassPathRessourceBulkAsync("kibana/stagemonitor-spans-kibana-index-pattern.bulk");
			elasticsearchClient.sendClassPathRessourceBulkAsync("kibana/Request-Analysis.bulk");
			elasticsearchClient.sendClassPathRessourceBulkAsync("kibana/Web-Analytics.bulk");

			elasticsearchClient.scheduleIndexManagement("stagemonitor-external-requests-",
					corePlugin.getMoveToColdNodesAfterDays(), deleteSpansAfterDays.getValue());
		}

		final Metric2Registry metricRegistry = initArguments.getMetricRegistry();
		final RequestMonitorPlugin requestMonitorPlugin = this;

		final Tracer tracer = ServiceLoader.load(TracerFactory.class, RequestMonitor.class.getClassLoader()).iterator().next().getTracer(initArguments);
		samplePriorityDeterminingSpanInterceptor = new SamplePriorityDeterminingSpanEventListener(initArguments.getConfiguration(), metricRegistry);
		final ServiceLoader<SpanEventListenerFactory> factories = ServiceLoader.load(SpanEventListenerFactory.class, RequestMonitorPlugin.class.getClassLoader());
		this.tracer = createSpanWrappingTracer(tracer, metricRegistry, requestMonitorPlugin, getRequestMonitor(),
				factories, samplePriorityDeterminingSpanInterceptor);
	}

	public static SpanWrappingTracer createSpanWrappingTracer(final Tracer delegate, final Metric2Registry metricRegistry,
															  final RequestMonitorPlugin requestMonitorPlugin,
															  final RequestMonitor requestMonitor,
															  final Iterable<SpanEventListenerFactory> spanInterceptorFactories,
															  final SamplePriorityDeterminingSpanEventListener samplePriorityDeterminingSpanInterceptor) {
		final SpanWrappingTracer spanWrappingTracer = new SpanWrappingTracer(delegate);
		spanWrappingTracer.addSpanInterceptor(new SpanContextInformation.SpanContextSpanEventListener(requestMonitor));
		spanWrappingTracer.addSpanInterceptor(samplePriorityDeterminingSpanInterceptor);
		spanWrappingTracer.addSpanInterceptor(new ExternalRequestMetricsSpanEventListener.Factory(metricRegistry, requestMonitorPlugin));
		spanWrappingTracer.addSpanInterceptor(new ServerRequestMetricsSpanEventListener.Factory(metricRegistry, requestMonitorPlugin));
		spanWrappingTracer.addSpanInterceptor(new AnonymizingSpanEventListener.MySpanEventListenerFactory(requestMonitorPlugin));
		spanWrappingTracer.addSpanInterceptor(new MDCSpanEventListener());
		for (SpanEventListenerFactory spanEventListenerFactory : spanInterceptorFactories) {
			spanWrappingTracer.addSpanInterceptor(spanEventListenerFactory);
		}
		return spanWrappingTracer;
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

	public boolean isCollectCpuTime() {
		return collectCpuTime.getValue();
	}

	public long getMinExecutionTimeNanos() {
		return minExecutionTimeNanos.getValue();
	}

	public double getOnlyCollectNCallTreesPerMinute() {
		return onlyCollectNCallTreesPerMinute.getValue();
	}

	public boolean isLogSpans() {
		return logSpans.getValue();
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
		if (requestMonitor != null) {
			requestMonitor.close();
		}
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

	public Collection<String> getOnlyReportSpansWithName() {
		return onlyReportSpansWithName.getValue();
	}

	public double getRateLimitServerSpansPerMinute() {
		return rateLimitServerSpansPerMinute.getValue();
	}

	public ConfigurationOption<Double> getRateLimitServerSpansPerMinuteOption() {
		return rateLimitServerSpansPerMinute;
	}

	public boolean isOnlyLogElasticsearchSpanReports() {
		return onlyLogElasticsearchSpanReports.getValue();
	}

	public double getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests() {
		return excludeCallTreeFromReportWhenFasterThanXPercentOfRequests.getValue();
	}

	public Collection<String> getUnnestExceptions() {
		return unnestExceptions.getValue();
	}

	public boolean isProfilerObjectPoolingActive() {
		return profilerObjectPooling.getValue();
	}

	public Collection<Pattern> getConfidentialParameters() {
		return confidentialParameters.getValue();
	}

	public static Map<String, String> getSafeParameterMap(Map<String, String> parameterMap, Collection<Pattern> confidentialParams) {
		Map<String, String> params = new LinkedHashMap<String, String>();
		for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
			final boolean paramExcluded = isParamExcluded(entry.getKey(), confidentialParams);
			if (paramExcluded) {
				params.put(entry.getKey(), "XXXX");
			} else {
				final String value = String.valueOf(entry.getValue());
				params.put(entry.getKey(), value.substring(0, Math.min(255, value.length())));
			}
		}
		return params;
	}

	private static boolean isParamExcluded(String queryParameter, Collection<Pattern> confidentialParams) {
		for (Pattern excludedParam : confidentialParams) {
			if (excludedParam.matcher(queryParameter).matches()) {
				return true;
			}
		}
		return false;
	}

	public Collection<String> getIgnoreExceptions() {
		return ignoreExceptions.getValue();
	}

	public double getRateLimitClientSpansPerMinute() {
		return rateLimitClientSpansPerMinute.getValue();
	}

	public ConfigurationOption<Double> getRateLimitClientSpansPerMinuteOption() {
		return rateLimitClientSpansPerMinute;
	}

	public double getExcludeExternalRequestsWhenFasterThanXPercent() {
		return excludeExternalRequestsWhenFasterThanXPercent.getValue();
	}

	public double getExcludeExternalRequestsFasterThan() {
		return excludeExternalRequestsFasterThan.getValue();
	}

	public boolean isReportAsync() {
		return reportSpansAsync.getValue();
	}

	public void addSpanInterceptor(SpanEventListenerFactory spanEventListenerFactory) {
		tracer.addSpanInterceptor(spanEventListenerFactory);
	}

	/**
	 * Add an {@link PreExecutionSpanInterceptor} to the interceptor list
	 *
	 * @param interceptor the interceptor that should be executed before measurement starts
	 */
	public void registerPreInterceptor(PreExecutionSpanInterceptor interceptor) {
		samplePriorityDeterminingSpanInterceptor.addPreInterceptor(interceptor);
	}

	/**
	 * Add an {@link PostExecutionSpanInterceptor} to the interceptor list
	 *
	 * @param interceptor the interceptor that should be executed before each report
	 */
	public void registerPostInterceptor(PostExecutionSpanInterceptor interceptor) {
		samplePriorityDeterminingSpanInterceptor.addPostInterceptor(interceptor);
	}

	public boolean isMonitorScheduledTasks() {
		return monitorScheduledTasks.getValue();
	}
}
