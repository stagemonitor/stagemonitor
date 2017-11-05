package org.stagemonitor.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.converter.DoubleValueConverter;
import org.stagemonitor.configuration.converter.StringValueConverter;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.grafana.GrafanaClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.ExecutorUtils;
import org.stagemonitor.tracing.anonymization.AnonymizingSpanEventListener;
import org.stagemonitor.tracing.impl.DefaultTracerFactory;
import org.stagemonitor.tracing.mdc.MDCSpanEventListener;
import org.stagemonitor.tracing.metrics.MetricsSpanEventListener;
import org.stagemonitor.tracing.profiler.CallTreeSpanEventListener;
import org.stagemonitor.tracing.profiler.formatter.AsciiCallTreeSignatureFormatter;
import org.stagemonitor.tracing.profiler.formatter.ShortSignatureFormatter;
import org.stagemonitor.tracing.reporter.ReportingSpanEventListener;
import org.stagemonitor.tracing.reporter.SpanReporter;
import org.stagemonitor.tracing.sampling.PostExecutionSpanInterceptor;
import org.stagemonitor.tracing.sampling.PreExecutionSpanInterceptor;
import org.stagemonitor.tracing.sampling.SamplePriorityDeterminingSpanEventListener;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanEventListenerFactory;
import org.stagemonitor.tracing.wrapper.SpanWrappingTracer;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Pattern;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.util.GlobalTracer;

import static org.stagemonitor.core.util.Assert.checkArgument;

public class TracingPlugin extends StagemonitorPlugin {

	private static final Logger logger = LoggerFactory.getLogger(TracingPlugin.class);

	private static final String TRACING_PLUGIN = "Tracing Plugin";

	/* What/how to monitor */
	private final ConfigurationOption<BusinessTransactionNamingStrategy> businessTransactionNamingStrategy = ConfigurationOption.enumOption(BusinessTransactionNamingStrategy.class)
			.key("stagemonitor.businessTransaction.namingStrategy")
			.dynamic(false)
			.label("Business Transaction naming strategy")
			.description("Defines how to name a business transaction that was detected by a method call. " +
					"For example a Spring-MVC controller method or a method that is annotated with @" + Traced.class.getSimpleName() + ". " +
					BusinessTransactionNamingStrategy.METHOD_NAME_SPLIT_CAMEL_CASE + ": Say Hello " +
					BusinessTransactionNamingStrategy.CLASS_NAME_DOT_METHOD_NAME + ": HelloController.sayHello " +
					BusinessTransactionNamingStrategy.CLASS_NAME_HASH_METHOD_NAME + ": HelloController#sayHello ")
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(BusinessTransactionNamingStrategy.METHOD_NAME_SPLIT_CAMEL_CASE);
	private final ConfigurationOption<Boolean> monitorScheduledTasks = ConfigurationOption.booleanOption()
			.key("stagemonitor.tracing.monitorScheduledTasks")
			.aliasKeys("stagemonitor.requestmonitor.monitorScheduledTasks")
			.dynamic(false)
			.label("Monitor scheduled tasks")
			.description("Set to true trace EJB (@Schedule) and Spring (@Scheduled) scheduled tasks.")
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(false);
	private final ConfigurationOption<Boolean> monitorAsyncInvocations = ConfigurationOption.booleanOption()
			.key("stagemonitor.tracing.monitorAsyncInvocations")
			.aliasKeys("stagemonitor.requestmonitor.monitorAsyncInvocations")
			.dynamic(false)
			.label("Monitor async invocations")
			.description("Set to true trace EJB (@Asynchronous) and Spring (@Async) async invocations.")
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(false);
	private final ConfigurationOption<Collection<Pattern>> confidentialParameters = ConfigurationOption.regexListOption()
			.key("stagemonitor.tracing.params.confidential.regex")
			.aliasKeys("stagemonitor.requestmonitor.requestparams.confidential.regex", "stagemonitor.requestmonitor.params.confidential.regex")
			.dynamic(true)
			.label("Confidential parameters (regex)")
			.description("A list of request parameter name patterns that should not be collected.\n" +
					"In the context of a HTTP request, a request parameter is either a query string or a application/x-www-form-urlencoded request " +
					"body (POST form content). In the context of a method invocation monitored with @Traced," +
					"this refers to the parameter name of the monitored method. Note that you have to compile your classes" +
					"with 'vars' debug information.")
			.tags("security-relevant")
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(Arrays.asList(
					Pattern.compile("(?i).*pass.*"),
					Pattern.compile("(?i).*credit.*"),
					Pattern.compile("(?i).*pwd.*"),
					Pattern.compile("(?i)pw")));
	private final ConfigurationOption<Collection<String>> excludedTags = ConfigurationOption.stringsOption()
			.key("stagemonitor.tracing.tags.excluded")
			.dynamic(true)
			.label("Excluded tags")
			.description("A list of tags which should not be reported. Note that is not possible to exclude boolean tags. " +
					"For example, if you don't want a particular call tree format to be reported, add " +
					SpanUtils.CALL_TREE_JSON + " or " + SpanUtils.CALL_TREE_ASCII + " accordingly.")
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(Collections.<String>emptySet());

	/* Profiler */
	private final ConfigurationOption<Boolean> profilerActive = ConfigurationOption.booleanOption()
			.key("stagemonitor.profiler.active")
			.dynamic(false)
			.label("Activate Profiler")
			.description("Whether or not the call tree profiler should be active.")
			.tags("profiler")
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(true);
	private final ConfigurationOption<Long> minExecutionTimeNanos = ConfigurationOption.longOption()
			.key("stagemonitor.profiler.minExecutionTimeNanos")
			.dynamic(false)
			.label("Min execution time (nanos)")
			.description("Don't show methods that executed faster than this value in the call tree (1 ms = 1,000,000 ns).")
			.tags("profiler")
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(100000L);
	private final ConfigurationOption<Double> minExecutionTimePercent = ConfigurationOption.doubleOption()
			.key("stagemonitor.profiler.minExecutionTimePercent")
			.dynamic(true)
			.label("Min execution time (%)")
			.description("Don't show methods that executed faster than this value in the call tree (0.5 or 0,5 means 0.5%).")
			.tags("profiler")
			.configurationCategory(TRACING_PLUGIN)
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
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(false);
	private final ConfigurationOption<Double> profilerRateLimitPerMinute = ConfigurationOption.doubleOption()
			.key("stagemonitor.profiler.sampling.rateLimitPerMinute")
			.aliasKeys("stagemonitor.requestmonitor.onlyCollectNCallTreesPerMinute")
			.dynamic(true)
			.label("Only report N call trees per minute")
			.description("Limits the rate at which call trees are collected. " +
					"Set to a value below 1 to deactivate call tree recording and to 1000000 or higher to always collect.")
			.tags("profiler", "sampling")
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(1000000d);
	private final ConfigurationOption<AsciiCallTreeSignatureFormatter> callTreeAsciiFormatter = ConfigurationOption.serviceLoaderStrategyOption(AsciiCallTreeSignatureFormatter.class)
			.key("stagemonitor.profiler.formatter")
			.dynamic(true)
			.label("Call Tree ASCII formatter")
			.description("Lets you choose how the method signatures in the ASCII call tree are formatted. " +
					"It's also possible to supply a custom implementation.")
			.tags("profiler", "advanced")
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(new ShortSignatureFormatter());

	/* Privacy */
	private final ConfigurationOption<Boolean> anonymizeIPs = ConfigurationOption.booleanOption()
			.key("stagemonitor.anonymizeIPs")
			.dynamic(true)
			.label("Anonymize IP Addresses")
			.description("For IPv4 addresses, the last octet is set to zero. " +
					"If the address is a IPv6 address, the last 80 bits (10 bytes) are set to zero. " +
					"This is just like Google Analytics handles IP anonymization.")
			.tags("privacy")
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(true);
	private final ConfigurationOption<Boolean> pseudonymizeUserName = ConfigurationOption.booleanOption()
			.key("stagemonitor.pseudonymize.username")
			.dynamic(true)
			.label("Pseudonymize Usernames")
			.description("Stagemonitor collects the user names which may be a privacy issue. " +
					"If set to true, the user name will be pseudonymized (SHA1 hashed).")
			.tags("privacy")
			.configurationCategory(TRACING_PLUGIN)
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
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(Collections.<String>emptySet());

	/* Reporting */
	private final ConfigurationOption<Boolean> logSpans = ConfigurationOption.booleanOption()
			.key("stagemonitor.tracing.reporting.log")
			.aliasKeys("stagemonitor.requestmonitor.reporting.log")
			.dynamic(true)
			.label("Log spans")
			.description("Whether or not spans should be logged.")
			.tags("reporting")
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(false);
	private final ConfigurationOption<Boolean> reportSpansAsync = ConfigurationOption.booleanOption()
			.key("stagemonitor.tracing.report.async")
			.aliasKeys("stagemonitor.requestmonitor.report.async")
			.dynamic(true)
			.label("Report Async")
			.description("Set to true to report collected spans asynchronously. It's recommended to always set this to " +
					"true. Otherwise the performance of your requests will suffer as spans are reported in band.")
			.configurationCategory(TRACING_PLUGIN)
			.tags("reporting", "advanced")
			.buildWithDefault(true);

	private final ConfigurationOption<Boolean> trackMetricsAsync = ConfigurationOption.booleanOption()
			.key("stagemonitor.tracing.metrics.async")
			.dynamic(true)
			.label("Track Metrics Async")
			.description("Set to true to track response time metrics asynchronously.")
			.configurationCategory(TRACING_PLUGIN)
			.tags("metircs", "advanced")
			.buildWithDefault(true);

	/* Exceptions */
	private final ConfigurationOption<Collection<String>> unnestExceptions = ConfigurationOption.stringsOption()
			.key("stagemonitor.tracing.unnestExeptions")
			.aliasKeys("stagemonitor.requestmonitor.unnestExeptions")
			.dynamic(true)
			.label("Unnest Exceptions")
			.description("Some Exceptions are so called 'nested exceptions' which wrap the actual cause of the exception. " +
					"A prominent example is Spring's NestedServletException. " +
					"In those cases it makes sense to unnest the exception to see the actual cause in the request analysis dashboard.")
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(Collections.singleton("org.springframework.web.util.NestedServletException"));
	private final ConfigurationOption<Collection<String>> ignoreExceptions = ConfigurationOption.stringsOption()
			.key("stagemonitor.tracing.ignoreExeptions")
			.aliasKeys("stagemonitor.requestmonitor.ignoreExeptions")
			.dynamic(true)
			.label("Ignore Exceptions")
			.description("The class names of exception to ignore. These exceptions won't show up in the span " +
					"and won't cause the error flag of the span to be set to true.")
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(Collections.<String>emptyList());

	/* Sampling */
	private final ConfigurationOption<Collection<String>> onlyReportSpansWithName = ConfigurationOption.stringsOption()
			.key("stagemonitor.tracing.sampling.onlyReportSpansWithName")
			.aliasKeys("stagemonitor.requestmonitor.onlyReportRequestsWithNameToElasticsearch")
			.dynamic(true)
			.label("Only report these operation names")
			.description("Limits the reporting of spans to operations with a certain name.")
			.tags("sampling")
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(Collections.<String>emptySet());
	private final ConfigurationOption<Double> defaultRateLimitSpansPerMinute = ConfigurationOption.doubleOption()
			.key("stagemonitor.tracing.sampling.rateLimitPerMinute.default")
			.dynamic(true)
			.label("Rate limit for spans per minute")
			.description("Limits the rate at which spans are sampled (collected and reported). " +
					"Set to a value <= 0 to sample no span and to 1000000 or higher to always sample. " +
					"This is useful in combination with the percantage based sampling to prevent overwhelming the " +
					"backend during peak times or DDOS attacks.\n" +
					"\n" +
					"Important: this setting is only evaluated for root spans so that a whole trace is either sampled" +
					"or not sampled.")
			.tags("sampling")
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(1000000d);
	private final ConfigurationOption<Double> defaultRateLimitSpansPercent = ConfigurationOption.doubleOption()
			.key("stagemonitor.tracing.sampling.percent.default")
			.dynamic(true)
			.label("Sampling probability for traces in %")
			.description("Sets the percentage of traces which are sampled (collected and reported). " +
					"When set to '0.01', only 1% of the traces will be sampled; when set to '1', all traces," +
					"including all spans will be sampled. Note that the maximum granularity is 1%.\n" +
					"\n" +
					"Important: this setting is only evaluated for root spans so that a whole trace is either sampled" +
					"or not sampled.")
			.tags("sampling")
			.addValidator(new ConfigurationOption.Validator<Double>() {
				@Override
				public void assertValid(Double rate) {
					checkArgument(rate >= 0.0 && rate <= 1, "rate should be between 0 and 1: was %s", rate);
				}
			})
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(1d);
	private final ConfigurationOption<Map<String, Double>> rateLimitSpansPerMinutePercentPerType = ConfigurationOption.mapOption(StringValueConverter.INSTANCE, DoubleValueConverter.INSTANCE)
			.key("stagemonitor.tracing.sampling.percent.perType")
			.dynamic(true)
			.label("Sampling probability for spans in % per operation type")
			.description("Sets the percentage of specific spans like 'jdbc' queries are collected and reported. " +
					"When set to '0.01', only 1% of the spans will be reported; when set to '1', all spans will be reported. " +
					"Note that the maximum granularity is 1%. " +
					"If your application makes excessive use of for example jdbc queries, you might want to deactivate " +
					"or rate limit the collection of spans.\n" +
					"Example: set `jdbc: 0.01` to only sample 1% of jdbc spans.\n" +
					"\n" +
					"Important: this setting is evaluated for every span (not only the root span), regardless of the" +
					"sampling decision of the trace. That means that you can exclude certain spans of a sampled trace" +
					"but also sample spans of a non sampled trace. Note that you will end up with incomplete traces.")
			.tags("sampling")
			.addValidator(new ConfigurationOption.Validator<Map<String, Double>>() {
				@Override
				public void assertValid(Map<String, Double> value) {
					for (double rate : value.values()) {
						checkArgument(rate >= 0.0 && rate <= 1, "rate should be between 0 and 1: was %s", rate);
					}
				}
			})
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(Collections.<String, Double>emptyMap());
	private final ConfigurationOption<Double> excludeCallTreeFromReportWhenFasterThanXPercentOfRequests = ConfigurationOption.doubleOption()
			.key("stagemonitor.tracing.sampling.excludeCallTreeFromReportWhenFasterThanXPercentOfRequests")
			.aliasKeys("stagemonitor.requestmonitor.elasticsearch.excludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests")
			.dynamic(true)
			.label("Exclude the Call Tree from reports on x% of the fastest requests")
			.description("Exclude the Call Tree from report when the request was faster faster than x " +
					"percent of requests with the same request name. This helps to reduce the network and disk overhead " +
					"as uninteresting Call Trees (those which are comparatively fast) are excluded. " +
					"Example: set to 1 to always exclude the Call Tree and to 0 to always include it. " +
					"With a setting of 0.85, the Call Tree will only be reported for the slowest 25% of the requests.")
			.tags("sampling")
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(0d);
	private final ConfigurationOption<Double> excludeExternalRequestsWhenFasterThanXPercent = ConfigurationOption.doubleOption()
			.key("stagemonitor.tracing.external.excludeExternalRequestsWhenFasterThanXPercent")
			.aliasKeys("stagemonitor.requestmonitor.external.excludeExternalRequestsWhenFasterThanXPercent")
			.dynamic(true)
			.label("Exclude external requests from reporting on x% of the fastest external requests")
			.description("Exclude the external request from reporting when the request was faster faster than x " +
					"percent of external requests with the same initiator (executedBy). This helps to reduce the network and disk overhead " +
					"as uninteresting external requests (those which are comparatively fast) are excluded." +
					"Example: set to 1 to always exclude the external request and to 0 to always include it. " +
					"With a setting of 0.85, the external request will only be reported for the slowest 25% of the requests.")
			.tags("external-requests", "sampling")
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(0d);
	private final ConfigurationOption<Double> excludeExternalRequestsFasterThan = ConfigurationOption.doubleOption()
			.key("stagemonitor.tracing.external.excludeExternalRequestsFasterThan")
			.aliasKeys("stagemonitor.requestmonitor.external.excludeExternalRequestsFasterThan")
			.dynamic(true)
			.label("Exclude external requests from reporting when faster than x ms")
			.description("Exclude the external request from reporting when the request was faster faster than x ms.")
			.tags("external-requests", "sampling")
			.configurationCategory(TRACING_PLUGIN)
			.buildWithDefault(0d);

	private static RequestMonitor requestMonitor;

	private SpanWrappingTracer spanWrappingTracer;
	private SamplePriorityDeterminingSpanEventListener samplePriorityDeterminingSpanInterceptor;
	private ReportingSpanEventListener reportingSpanEventListener;
	private CorePlugin corePlugin;
	private TracerFactory tracerFactory;

	/**
	 * @return the {@link Span} of the current request or a noop {@link Span} (never <code>null</code>)
	 */
	public static Span getCurrentSpan() {
		final Scope activeScope = GlobalTracer.get().scopeManager().active();
		if (activeScope != null) {
			return activeScope.span();
		} else {
			return NoopTracerFactory.create().buildSpan(null).startManual();
		}
	}

	/**
	 * This is an internal method, use {@link GlobalTracer#get()}
	 * <p>
	 * However, if you are a stagemonitor plugin developer, you should use this method
	 * as the tracer can be mocked more easily.
	 */
	public Tracer getTracer() {
		if (spanWrappingTracer != null && corePlugin.isStagemonitorActive()) {
			return spanWrappingTracer;
		} else {
			return NoopTracerFactory.create();
		}
	}

	@Override
	public void initializePlugin(final StagemonitorPlugin.InitArguments initArguments) {
		corePlugin = initArguments.getPlugin(CorePlugin.class);
		final ElasticsearchClient elasticsearchClient = corePlugin.getElasticsearchClient();
		final GrafanaClient grafanaClient = corePlugin.getGrafanaClient();

		if (corePlugin.isReportToElasticsearch()) {
			elasticsearchClient.sendClassPathRessourceBulkAsync("kibana/Request-Metrics.bulk", true);
			grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchRequestDashboard.json");
		}

		final Metric2Registry metricRegistry = initArguments.getMetricRegistry();
		tracerFactory = getTracerImpl();
		final Tracer tracer = tracerFactory.getTracer(initArguments);
		reportingSpanEventListener = new ReportingSpanEventListener(initArguments.getConfiguration());
		for (SpanReporter spanReporter : ServiceLoader.load(SpanReporter.class, RequestMonitor.class.getClassLoader())) {
			addReporter(spanReporter);
		}
		samplePriorityDeterminingSpanInterceptor = new SamplePriorityDeterminingSpanEventListener(initArguments.getConfiguration());
		final ServiceLoader<SpanEventListenerFactory> factories = ServiceLoader.load(SpanEventListenerFactory.class, TracingPlugin.class.getClassLoader());
		this.spanWrappingTracer = createSpanWrappingTracer(tracer, initArguments.getConfiguration(), metricRegistry,
				factories, samplePriorityDeterminingSpanInterceptor, reportingSpanEventListener);
		GlobalTracer.register(spanWrappingTracer);
	}

	@Override
	public List<Class<? extends StagemonitorPlugin>> dependsOn() {
		return Collections.<Class<? extends StagemonitorPlugin>>singletonList(CorePlugin.class);
	}

	private TracerFactory getTracerImpl() {
		final Iterator<TracerFactory> tracerFactoryIterator = ServiceLoader.load(TracerFactory.class, RequestMonitor.class.getClassLoader()).iterator();
		if (tracerFactoryIterator.hasNext()) {
			final TracerFactory tracerFactory = tracerFactoryIterator.next();
			assertIsSingleImplementation(tracerFactoryIterator, tracerFactory);
			return tracerFactory;
		} else {
			logger.info("No OpenTracing implementation found. Falling back to DefaultTracerImpl. " +
					"This is fine if you just want to use stagemonitor for development, for example with the in-browser-widget. " +
					"If you want to report your traces to Elasticsearch, add a dependency to stagemonitor-tracing-elasticsearch. " +
					"If you want to report to Zipkin, add stagemonitor-tracing-zipkin.");
			return new DefaultTracerFactory();
		}
	}

	private void assertIsSingleImplementation(Iterator<TracerFactory> tracerFactoryIterator, TracerFactory tracer) {
		if (tracerFactoryIterator.hasNext()) {
			final TracerFactory tracer2 = tracerFactoryIterator.next();
			throw new IllegalStateException(MessageFormat.format("Multiple tracing implementations found: {0}, {1}. " +
							"Make sure you only have one stagemonitor-tracing-* jar in your class path.",
					tracer.getClass().getName(), tracer2.getClass().getName()));
		}
	}

	public static SpanWrappingTracer createSpanWrappingTracer(final Tracer delegate, ConfigurationRegistry configuration, final Metric2Registry metricRegistry,
															  final Iterable<SpanEventListenerFactory> spanInterceptorFactories,
															  final SamplePriorityDeterminingSpanEventListener samplePriorityDeterminingSpanInterceptor,
															  final ReportingSpanEventListener reportingSpanEventListener) {
		final CorePlugin corePlugin = configuration.getConfig(CorePlugin.class);
		final TracingPlugin tracingPlugin = configuration.getConfig(TracingPlugin.class);
		final SpanWrappingTracer spanWrappingTracer = new SpanWrappingTracer(delegate);
		spanWrappingTracer.addEventListenerFactory(new SpanContextInformation.SpanContextSpanEventListener());
		spanWrappingTracer.addEventListenerFactory(samplePriorityDeterminingSpanInterceptor);
		spanWrappingTracer.addEventListenerFactory(new MDCSpanEventListener(corePlugin, tracingPlugin));
		spanWrappingTracer.addEventListenerFactory(new B3IdentifierTagger(tracingPlugin));
		for (SpanEventListenerFactory spanEventListenerFactory : spanInterceptorFactories) {
			spanWrappingTracer.addEventListenerFactory(spanEventListenerFactory);
		}
		final ThreadPoolExecutor singleThreadDeamonPool = ExecutorUtils.createSingleThreadDeamonPool("metric-tracking", 1000, corePlugin);
		final MetricsSpanEventListener spanEventListener = new MetricsSpanEventListener(metricRegistry, singleThreadDeamonPool, tracingPlugin);
		spanWrappingTracer.addEventListenerFactory(spanEventListener);
		spanWrappingTracer.addEventListenerFactory(new CallTreeSpanEventListener(corePlugin.getMetricRegistry(), tracingPlugin));
		spanWrappingTracer.addEventListenerFactory(new AnonymizingSpanEventListener(tracingPlugin));
		spanWrappingTracer.addEventListenerFactory(reportingSpanEventListener);
		spanWrappingTracer.addEventListenerFactory(new SpanContextInformation.SpanFinalizer());
		return spanWrappingTracer;
	}

	@Override
	public void registerWidgetMetricTabPlugins(WidgetMetricTabPluginsRegistry widgetMetricTabPluginsRegistry) {
		widgetMetricTabPluginsRegistry.addWidgetMetricTabPlugin("/stagemonitor/static/tabs/metrics/request-metrics");
	}

	public boolean isRoot(Span span) {
		return tracerFactory.isRoot(span);
	}

	public boolean isSampled(Span span) {
		return tracerFactory.isSampled(span);
	}

	public RequestMonitor getRequestMonitor() {
		if (requestMonitor == null) {
			requestMonitor = new RequestMonitor(Stagemonitor.getConfiguration(), Stagemonitor.getMetric2Registry());
		}
		return requestMonitor;
	}

	public long getMinExecutionTimeNanos() {
		return minExecutionTimeNanos.getValue();
	}

	public double getProfilerRateLimitPerMinute() {
		return profilerRateLimitPerMinute.getValue();
	}

	public ConfigurationOption<Double> getProfilerRateLimitPerMinuteOption() {
		return profilerRateLimitPerMinute;
	}

	public boolean isLogSpans() {
		return logSpans.getValue();
	}

	public boolean isProfilerActive() {
		return profilerActive.getValue();
	}

	public BusinessTransactionNamingStrategy getBusinessTransactionNamingStrategy() {
		return businessTransactionNamingStrategy.getValue();
	}

	@Override
	public void onShutDown() {
		if (reportingSpanEventListener != null) {
			reportingSpanEventListener.close();
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

	public double getDefaultRateLimitSpansPerMinute() {
		return defaultRateLimitSpansPerMinute.getValue();
	}

	public ConfigurationOption<Double> getDefaultRateLimitSpansPerMinuteOption() {
		return defaultRateLimitSpansPerMinute;
	}

	public Double getDefaultRateLimitSpansPercent() {
		return defaultRateLimitSpansPercent.getValue();
	}

	public Map<String, Double> getRateLimitSpansPerMinutePercentPerType() {
		return rateLimitSpansPerMinutePercentPerType.getValue();
	}

	public ConfigurationOption<Double> getDefaultRateLimitSpansPercentOption() {
		return defaultRateLimitSpansPercent;
	}

	public ConfigurationOption<Map<String, Double>> getRateLimitSpansPerMinutePercentPerTypeOption() {
		return rateLimitSpansPerMinutePercentPerType;
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

	public boolean isMonitorScheduledTasks() {
		return monitorScheduledTasks.getValue();
	}

	public boolean isMonitorAsyncInvocations() {
		return monitorAsyncInvocations.getValue();
	}

	public void addSpanEventListenerFactory(final SpanEventListenerFactory spanEventListenerFactory) {
		onInit(new Runnable() {
			@Override
			public void run() {
				if (spanWrappingTracer != null) {
					spanWrappingTracer.addEventListenerFactory(spanEventListenerFactory);
				}
			}
		});
	}

	public SpanWrappingTracer getSpanWrappingTracer() {
		return spanWrappingTracer;
	}

	public Collection<String> getExcludedTags() {
		return excludedTags.get();
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

	public void addReporter(SpanReporter spanReporter) {
		reportingSpanEventListener.addReporter(spanReporter);
	}

	public ReportingSpanEventListener getReportingSpanEventListener() {
		return reportingSpanEventListener;
	}

	public boolean isTrackMetricsAsync() {
		return trackMetricsAsync.getValue();
	}

	public AsciiCallTreeSignatureFormatter getCallTreeAsciiFormatter() {
		return callTreeAsciiFormatter.getValue();
	}

	public void registerDefaultRateLimitSpansPercentChangeListener(ConfigurationOption.ChangeListener<Double> changeListener) {
		defaultRateLimitSpansPercent.addChangeListener(changeListener);
	}

}
