package org.stagemonitor.web.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.converter.SetValueConverter;
import org.stagemonitor.configuration.converter.StringValueConverter;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.grafana.GrafanaClient;
import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.util.StringUtils;
import org.stagemonitor.web.servlet.configuration.ConfigurationServlet;
import org.stagemonitor.web.servlet.eum.ClientSpanExtension;
import org.stagemonitor.web.servlet.eum.ClientSpanJavaScriptServlet;
import org.stagemonitor.web.servlet.eum.ClientSpanMetadataTagProcessor.ClientSpanMetadataConverter;
import org.stagemonitor.web.servlet.eum.ClientSpanMetadataTagProcessor.ClientSpanMetadataDefinition;
import org.stagemonitor.web.servlet.eum.ClientSpanServlet;
import org.stagemonitor.web.servlet.filter.HttpRequestMonitorFilter;
import org.stagemonitor.web.servlet.filter.StagemonitorSecurityFilter;
import org.stagemonitor.web.servlet.health.HealthCheckServlet;
import org.stagemonitor.web.servlet.initializer.ServletContainerInitializerUtil;
import org.stagemonitor.web.servlet.initializer.StagemonitorServletContainerInitializer;
import org.stagemonitor.web.servlet.session.SessionCounter;
import org.stagemonitor.web.servlet.widget.SpanServlet;
import org.stagemonitor.web.servlet.widget.StagemonitorMetricsServlet;
import org.stagemonitor.web.servlet.widget.WidgetServlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Pattern;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;

import static org.stagemonitor.core.pool.MBeanPooledResource.tomcatThreadPools;
import static org.stagemonitor.core.pool.PooledResourceMetricsRegisterer.registerPooledResources;

public class ServletPlugin extends StagemonitorPlugin {

	public static final String STAGEMONITOR_SHOW_WIDGET = "X-Stagemonitor-Show-Widget";

	private static final String WEB_PLUGIN = "Servlet Plugin";

	private static final Logger logger = LoggerFactory.getLogger(ServletPlugin.class);

	private Map<String, ClientSpanMetadataDefinition> whitelistedClientSpanTagsFromSPI;

	private final ConfigurationOption<Collection<Pattern>> requestParamsConfidential = ConfigurationOption.regexListOption()
			.key("stagemonitor.requestmonitor.http.requestparams.confidential.regex")
			.dynamic(true)
			.label("Deprecated: Confidential request parameters (regex)")
			.description("Deprecated, use stagemonitor.tracing.params.confidential.regex instead." +
					"A list of request parameter name patterns that should not be collected.\n" +
					"A request parameter is either a query string or a application/x-www-form-urlencoded request " +
					"body (POST form content)")
			.tags("security-relevant", "deprecated")
			.configurationCategory(WEB_PLUGIN)
			.buildWithDefault(Arrays.asList(
					Pattern.compile("(?i).*pass.*"),
					Pattern.compile("(?i).*credit.*"),
					Pattern.compile("(?i).*pwd.*")));
	private ConfigurationOption<Boolean> collectHttpHeaders = ConfigurationOption.booleanOption()
			.key("stagemonitor.requestmonitor.http.collectHeaders")
			.dynamic(true)
			.label("Collect HTTP headers")
			.description("Whether or not HTTP headers should be collected with a call stack.")
			.configurationCategory(WEB_PLUGIN)
			.tags("security-relevant")
			.buildWithDefault(true);
	private ConfigurationOption<Boolean> parseUserAgent = ConfigurationOption.booleanOption()
			.key("stagemonitor.requestmonitor.http.parseUserAgent")
			.dynamic(true)
			.label("Analyze user agent")
			.description("Whether or not the user-agent header should be parsed and analyzed to get information " +
					"about the browser, device type and operating system. If you want to enable this option, you have " +
					"to add a dependency on net.sf.uadetector:uadetector-resources:2014.10. As this library is no longer " +
					"maintained, it is however recommended to use the Elasticsearch ingest user agent plugin. See " +
					"https://www.elastic.co/guide/en/elasticsearch/plugins/master/ingest-user-agent.html")
			.tags("deprecated")
			.configurationCategory(WEB_PLUGIN)
			.buildWithDefault(false);
	private ConfigurationOption<Collection<String>> excludeHeaders = ConfigurationOption.lowerStringsOption()
			.key("stagemonitor.requestmonitor.http.headers.excluded")
			.dynamic(true)
			.label("Do not collect headers")
			.description("A list of (case insensitive) header names that should not be collected.")
			.configurationCategory(WEB_PLUGIN)
			.tags("security-relevant")
			.buildWithDefault(new LinkedHashSet<String>(Arrays.asList("cookie", "authorization", STAGEMONITOR_SHOW_WIDGET)));
	private final ConfigurationOption<Boolean> widgetEnabled = ConfigurationOption.booleanOption()
			.key("stagemonitor.web.widget.enabled")
			.dynamic(true)
			.label("In browser widget enabled")
			.description("If active, stagemonitor will inject a widget in the web site containing the call tree. " +
					"If disabled, you can still enable it for authorized users by sending the HTTP header " +
					"`X-Stagemonitor-Show-Widget: <stagemonitor.password>`. You can use browser plugins like Modify " +
					"Headers for this. Note: if `stagemonitor.password` is set to an empty string, you can't disable the widget.\n" +
					"Requires Servlet-Api >= 3.0")
			.configurationCategory(WEB_PLUGIN)
			.buildWithDefault(true);
	private final ConfigurationOption<Map<Pattern, String>> groupUrls = ConfigurationOption.regexMapOption()
			.key("stagemonitor.groupUrls")
			.dynamic(true)
			.label("Group URLs regex")
			.description("Combine url paths by regex to a single url group.\n" +
					"E.g. `(.*).js: *.js` combines all URLs that end with `.js` to a group named `*.js`. " +
					"The metrics for all URLs matching the pattern are consolidated and shown in one row in the request table. " +
					"The syntax is `<regex>: <group name>[, <regex>: <group name>]*`")
			.configurationCategory(WEB_PLUGIN)
			.buildWithDefault(new LinkedHashMap<Pattern, String>() {{
				put(Pattern.compile("(.*).js$"), "*.js");
				put(Pattern.compile("(.*).css$"), "*.css");
				put(Pattern.compile("(.*).jpg$"), "*.jpg");
				put(Pattern.compile("(.*).jpeg$"), "*.jpeg");
				put(Pattern.compile("(.*).png$"), "*.png");
			}});
	private final ConfigurationOption<Boolean> collectPageLoadTimesPerRequest = ConfigurationOption.booleanOption()
			.key("stagemonitor.web.collectPageLoadTimesPerRequest")
			.dynamic(true)
			.label("Collect Page Load Time data per request group")
			.description("Whether or not browser, network and overall execution time should be collected per request group.\n" +
					"If set to true, four additional timers will be created for each request group to record the page " +
					"rendering time, dom processing time, network time and overall time per request. " +
					"If set to false, the times of all requests will be aggregated.")
			.configurationCategory(WEB_PLUGIN)
			.buildWithDefault(false);
	private final ConfigurationOption<Collection<String>> excludedRequestPaths = ConfigurationOption.stringsOption()
			.key("stagemonitor.web.paths.excluded")
			.dynamic(true)
			.label("Excluded paths (ant path style)")
			.description("Request paths that should not be monitored. " +
					"A value of `/aaa` means, that all paths starting with `/aaa` should not be monitored." +
					" It's recommended to not monitor static resources, as they are typically not interesting to " +
					"monitor but consume resources when you do.")
			.configurationCategory(WEB_PLUGIN)
			.buildWithDefault(SetValueConverter.immutableSet(
					// exclude paths of static vaadin resources
					"/VAADIN/",
					// don't monitor vaadin heatbeat
					"/HEARTBEAT/",
					"/favicon.ico"));
	private final ConfigurationOption<Collection<String>> excludedRequestPathsAntPattern = ConfigurationOption.stringsOption()
			.key("stagemonitor.web.paths.excluded.antPattern")
			.dynamic(true)
			.label("Excluded paths (ant path style)")
			.description("Request paths that should not be monitored. " +
					"A value of `/**/*.js` means, that all paths ending with `.js` should not be monitored." +
					" It's recommended to not monitor static resources, as they are typically not interesting to " +
					"monitor but consume resources when you do. For more documentation, refer to " +
					"https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/util/AntPathMatcher.html")
			.configurationCategory(WEB_PLUGIN)
			.buildWithDefault(SetValueConverter.immutableSet(
					"/**/*.js",
					"/**/*.css",
					"/**/*.jpg",
					"/**/*.jpeg",
					"/**/*.png"));
	private final ConfigurationOption<String> metricsServletAllowedOrigin = ConfigurationOption.stringOption()
			.key("stagemonitor.web.metricsServlet.allowedOrigin")
			.dynamic(true)
			.label("Allowed origin")
			.description("The Access-Control-Allow-Origin header value for the metrics servlet.")
			.configurationCategory(WEB_PLUGIN)
			.build();
	private final ConfigurationOption<String> metricsServletJsonpParameter = ConfigurationOption.stringOption()
			.key("stagemonitor.web.metricsServlet.jsonpParameter")
			.dynamic(true)
			.label("The Jsonp callback parameter name")
			.description("The name of the parameter used to specify the jsonp callback.")
			.configurationCategory(WEB_PLUGIN)
			.build();
	private ConfigurationOption<Boolean> monitorOnlySpringMvcOption = ConfigurationOption.booleanOption()
			.key("stagemonitor.requestmonitor.spring.monitorOnlySpringMvcRequests")
			.dynamic(true)
			.label("Monitor only SpringMVC requests")
			.description("Whether or not requests should be ignored, if they will not be handled by a Spring MVC controller method.\n" +
					"This is handy, if you are not interested in the performance of serving static files. " +
					"Setting this to true can also significantly reduce the amount of files (and thus storing space) " +
					"Graphite will allocate.")
			.configurationCategory(WEB_PLUGIN)
			.buildWithDefault(false);
	private ConfigurationOption<Boolean> monitorOnlyResteasyOption = ConfigurationOption.booleanOption()
			.key("stagemonitor.requestmonitor.resteasy.monitorOnlyResteasyRequests")
			.dynamic(true)
			.label("Monitor only Resteasy reqeusts")
			.description("Whether or not requests should be ignored, if they will not be handled by a Resteasy resource method.\n" +
					"This is handy, if you are not interested in the performance of serving static files. " +
					"Setting this to true can also significantly reduce the amount of files (and thus storing space) " +
					"Graphite will allocate.")
			.configurationCategory(WEB_PLUGIN)
			.buildWithDefault(false);
	private ConfigurationOption<Collection<String>> requestExceptionAttributes = ConfigurationOption.stringsOption()
			.key("stagemonitor.requestmonitor.requestExceptionAttributes")
			.dynamic(true)
			.label("Request Exception Attributes")
			.description("Defines the list of attribute names to check on the HttpServletRequest when searching for an exception. \n\n" +
					"Stagemonitor searches this list in order to see if any of these attributes are set on the request with " +
					"an Exception object and then records that information on the span. If your web framework " +
					"sets a different attribute outside of the defaults, you can add that attribute to this list to properly " +
					"record the exception on the trace.")
			.configurationCategory(WEB_PLUGIN)
			.buildWithDefault(new LinkedHashSet<String>() {{
				add("javax.servlet.error.exception");
				add("exception");
				add("org.springframework.web.servlet.DispatcherServlet.EXCEPTION");
			}});
	private ConfigurationOption<Boolean> honorDoNotTrackHeader = ConfigurationOption.booleanOption()
			.key("stagemonitor.web.honorDoNotTrackHeader")
			.dynamic(true)
			.label("Honor do not track header")
			.description("When set to true, requests that include the dnt header won't be reported. " +
					"Depending on your use case you might not be required to stop reporting spans even " +
					"if dnt is set. See https://tools.ietf.org/html/draft-mayer-do-not-track-00#section-9.3")
			.tags("privacy")
			.configurationCategory(WEB_PLUGIN)
			.buildWithDefault(false);
	private final ConfigurationOption<Boolean> clientSpansEnabled = ConfigurationOption.booleanOption()
			.key("stagemonitor.eum.enabled")
			.dynamic(true)
			.label("Enable End User Spans")
			.description("The End User Monitoring feature collects the browser, network and overall perceived " +
					"execution time from the user's perspective. When activated, this application will be able to " +
					"receive client spans. You probably want to enable `stagemonitor.eum.injection.enabled` too.")
			.configurationCategory(WEB_PLUGIN)
			.buildWithDefault(false);
	private ConfigurationOption<Boolean> clientSpanInjectionEnabled = ConfigurationOption.booleanOption()
			.key("stagemonitor.eum.injection.enabled")
			.dynamic(true)
			.label("Enable End User script injection")
			.description("If enabled, stagemonitor will inject the client span collection scripts in the loaded page.")
			.configurationCategory(WEB_PLUGIN)
			.buildWithDefault(true);
	private ConfigurationOption<Map<String, ClientSpanMetadataDefinition>> whitelistedClientSpanTags = ConfigurationOption.mapOption(StringValueConverter.INSTANCE, new ClientSpanMetadataConverter())
			.key("stagemonitor.eum.whitelistedClientSpanTags")
			.dynamic(true)
			.label("Whitelisted client span tags")
			.description("Defines the list of client span tags, which a client shall be allowed to sent. Tags may be added by" +
					" plugins and this configuration option. Tags neither provided by a plugin nor added by this" +
					" configuration option are ignored. Syntax is `key: type`. Valid types are string, boolean and number." +
					" Example: `user: string, logged_in: boolean, age: number`")
			.configurationCategory(WEB_PLUGIN)
			.buildWithDefault(Collections.<String, ClientSpanMetadataDefinition>emptyMap());
	private ConfigurationOption<Boolean> minifyClientSpanScript = ConfigurationOption.booleanOption()
			.key("stagemonitor.eum.debugCollectionScript")
			.dynamic(true)
			.label("Use debug build of weasel")
			.description("If set, stagemonitor will serve the debug build of weasel for end user monitoring." +
					" This should only be set to true, if you debug errors in the end user monitoring.")
			.configurationCategory(WEB_PLUGIN)
			.tags("advanced")
			.buildWithDefault(false);
	private ConfigurationOption<Integer> clientSpanScriptCacheDuration = ConfigurationOption.integerOption()
			.key("stagemonitor.eum.clientSpanScriptCacheDuration")
			.dynamic(true)
			.label("Client Span Script Cache Duration")
			.description("This configuration option sets how long" +
					" the script shall be cached by browsers. The value entered is the cache duration in minutes." +
					" Entering 0 or a negative value will result in transmitting 'Cache-Control: no-cache'.")
			.configurationCategory(WEB_PLUGIN)
			.tags("advanced")
			.buildWithDefault(5);
	private ClientSpanJavaScriptServlet clientSpanJavaScriptServlet;
	private List<ClientSpanExtension> clientSpanExtensions;

	@Override
	public void initializePlugin(StagemonitorPlugin.InitArguments initArguments) {
		registerPooledResources(initArguments.getMetricRegistry(), tomcatThreadPools());
		final CorePlugin corePlugin = initArguments.getPlugin(CorePlugin.class);
		ElasticsearchClient elasticsearchClient = corePlugin.getElasticsearchClient();
		if (corePlugin.isReportToElasticsearch()) {
			final GrafanaClient grafanaClient = corePlugin.getGrafanaClient();
			elasticsearchClient.sendClassPathRessourceBulkAsync("kibana/Application-Server.bulk", true);
			grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchApplicationServer.json");
		}
		initClientSpanExtensions(initArguments.getConfiguration());
	}

	@Override
	public List<Class<? extends StagemonitorPlugin>> dependsOn() {
		return Collections.<Class<? extends StagemonitorPlugin>>singletonList(TracingPlugin.class);
	}

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		final List<ConfigurationOption<?>> configurationOptions = super.getConfigurationOptions();
		if (!ClassUtils.isPresent("org.springframework.web.servlet.HandlerMapping")) {
			configurationOptions.remove(monitorOnlySpringMvcOption);
		}

		if (!ClassUtils.isPresent("org.jboss.resteasy.core.ResourceMethodRegistry")) {
			configurationOptions.remove(monitorOnlyResteasyOption);
		}

		return configurationOptions;
	}

	public boolean isCollectHttpHeaders() {
		return collectHttpHeaders.getValue();
	}

	public boolean isParseUserAgent() {
		return parseUserAgent.getValue();
	}

	public Collection<String> getExcludeHeaders() {
		return excludeHeaders.getValue();
	}

	public boolean isWidgetEnabled() {
		return widgetEnabled.getValue();
	}

	public Map<Pattern, String> getGroupUrls() {
		return groupUrls.getValue();
	}

	public Collection<Pattern> getRequestParamsConfidential() {
		return requestParamsConfidential.getValue();
	}

	public boolean isClientSpanCollectionEnabled() {
		return clientSpansEnabled.getValue();
	}

	public boolean isClientSpanCollectionInjectionEnabled() {
		return clientSpanInjectionEnabled.getValue();
	}

	public boolean getMinifyClientSpanScript() {
		return minifyClientSpanScript.getValue();
	}

	public void registerMinifyClientSpanScriptOptionChangedListener(ConfigurationOption.ChangeListener<Boolean> listener) {
		minifyClientSpanScript.addChangeListener(listener);
	}

	public int getClientSpanScriptCacheDuration() {
		return clientSpanScriptCacheDuration.getValue();
	}

	public boolean isCollectPageLoadTimesPerRequest() {
		return collectPageLoadTimesPerRequest.getValue();
	}

	public ConfigurationOption<Collection<String>> getExcludedRequestPaths() {
		return excludedRequestPaths;
	}

	public ConfigurationOption<Collection<String>> getExcludedRequestPathsAntPattern() {
		return excludedRequestPathsAntPattern;
	}

	public String getMetricsServletAllowedOrigin() {
		return metricsServletAllowedOrigin.getValue();
	}

	public String getMetricsServletJsonpParamName() {
		return metricsServletJsonpParameter.getValue();
	}

	public boolean isWidgetAndStagemonitorEndpointsAllowed(HttpServletRequest request, ConfigurationRegistry configuration) {
		final Boolean showWidgetAttr = (Boolean) request.getAttribute(STAGEMONITOR_SHOW_WIDGET);
		if (showWidgetAttr != null) {
			logger.debug("isWidgetAndStagemonitorEndpointsAllowed: showWidgetAttr={}", showWidgetAttr);
			return showWidgetAttr;
		}

		final boolean widgetEnabled = isWidgetEnabled();
		final boolean passwordInShowWidgetHeaderCorrect = isPasswordInShowWidgetHeaderCorrect(request, configuration);
		final boolean result = widgetEnabled || passwordInShowWidgetHeaderCorrect;
		logger.debug("isWidgetAndStagemonitorEndpointsAllowed: isWidgetEnabled={}, isPasswordInShowWidgetHeaderCorrect={}, result={}",
				widgetEnabled, passwordInShowWidgetHeaderCorrect, result);
		return result;
	}

	private boolean isPasswordInShowWidgetHeaderCorrect(HttpServletRequest request, ConfigurationRegistry configuration) {
		String password = request.getHeader(STAGEMONITOR_SHOW_WIDGET);
		if (configuration.isPasswordCorrect(password)) {
			return true;
		} else {
			if (StringUtils.isNotEmpty(password)) {
				logger.error("The password transmitted via the header {} is not correct. " +
								"This might be a malicious attempt to guess the value of {}. " +
								"The request was initiated from the ip {}.",
						STAGEMONITOR_SHOW_WIDGET, Stagemonitor.STAGEMONITOR_PASSWORD,
						MonitoredHttpRequest.getClientIp(request));
			}
			return false;
		}
	}

	public boolean isMonitorOnlySpringMvcRequests() {
		return monitorOnlySpringMvcOption.getValue();
	}

	public boolean isMonitorOnlyResteasyRequests() {
		return monitorOnlyResteasyOption.getValue();
	}

	public Collection<String> getRequestExceptionAttributes() {
		return requestExceptionAttributes.getValue();
	}

	public boolean isHonorDoNotTrackHeader() {
		return honorDoNotTrackHeader.getValue();
	}

	public Map<String, ClientSpanMetadataDefinition> getWhitelistedClientSpanTags() {
		HashMap<String, ClientSpanMetadataDefinition> allWhitelistedClientSpanTags = new HashMap<String, ClientSpanMetadataDefinition>();
		allWhitelistedClientSpanTags.putAll(whitelistedClientSpanTagsFromSPI);
		allWhitelistedClientSpanTags.putAll(whitelistedClientSpanTags.get());
		return Collections.unmodifiableMap(allWhitelistedClientSpanTags);
	}

	private void initClientSpanExtensions(ConfigurationRegistry config) {
		List<ClientSpanExtension> clientSpanExtensions = new ArrayList<ClientSpanExtension>();
		for (ClientSpanExtension clientSpanExtension : ServiceLoader.load(ClientSpanExtension.class)) {
			clientSpanExtension.init(config);
			clientSpanExtensions.add(clientSpanExtension);
		}
		this.clientSpanExtensions = clientSpanExtensions;
		initWhiteListedClientSpanTags(clientSpanExtensions);
	}

	private void initWhiteListedClientSpanTags(List<ClientSpanExtension> clientSpanExtensions) {
		HashMap<String, ClientSpanMetadataDefinition> whitelistedTagsFromSPI = new HashMap<String, ClientSpanMetadataDefinition>();
		for (ClientSpanExtension clientSpanExtension : clientSpanExtensions) {
			final Map<String, ClientSpanMetadataDefinition> whitelistedTags = clientSpanExtension.getWhitelistedTags();
			whitelistedTagsFromSPI.putAll(whitelistedTags);
		}
		this.whitelistedClientSpanTagsFromSPI = Collections.unmodifiableMap(whitelistedTagsFromSPI);
	}

	public List<ClientSpanExtension> getClientSpanExtenders() {
		return clientSpanExtensions;
	}

	private void setClientSpanJavaScriptServlet(ClientSpanJavaScriptServlet clientSpanJavaScriptServlet) {
		this.clientSpanJavaScriptServlet = clientSpanJavaScriptServlet;
	}

	public ClientSpanJavaScriptServlet getClientSpanJavaScriptServlet() {
		return clientSpanJavaScriptServlet;
	}

	public static class Initializer implements StagemonitorServletContainerInitializer {
		@Override
		public void onStartup(ServletContext ctx) {
			Stagemonitor.init();
			if (ServletContainerInitializerUtil.avoidDoubleInit(this, ctx)) return;
			ctx.addServlet(ConfigurationServlet.class.getSimpleName(), new ConfigurationServlet())
					.addMapping(ConfigurationServlet.CONFIGURATION_ENDPOINT);
			ctx.addServlet(StagemonitorMetricsServlet.class.getSimpleName(), new StagemonitorMetricsServlet())
					.addMapping("/stagemonitor/metrics");
			ctx.addServlet(ClientSpanServlet.class.getSimpleName(), new ClientSpanServlet())
					.addMapping("/stagemonitor/public/eum");
			final ClientSpanJavaScriptServlet servlet = new ClientSpanJavaScriptServlet();
			Stagemonitor.getPlugin(ServletPlugin.class).setClientSpanJavaScriptServlet(servlet);
			ctx.addServlet(ClientSpanJavaScriptServlet.class.getSimpleName(), servlet)
					.addMapping("/stagemonitor/public/eum.js");
			ctx.addServlet(StagemonitorFileServlet.class.getSimpleName(), new StagemonitorFileServlet())
					.addMapping("/stagemonitor/static/*", "/stagemonitor/public/static/*");
			ctx.addServlet(WidgetServlet.class.getSimpleName(), new WidgetServlet())
					.addMapping("/stagemonitor");

			final ServletRegistration.Dynamic spanServlet = ctx.addServlet(SpanServlet.class.getSimpleName(), new SpanServlet());
			spanServlet.addMapping("/stagemonitor/spans");
			spanServlet.setAsyncSupported(true);


			final FilterRegistration.Dynamic securityFilter = ctx.addFilter(StagemonitorSecurityFilter.class.getSimpleName(), new StagemonitorSecurityFilter());
			// Add as last filter so that other filters have the chance to set the
			// ServletPlugin.STAGEMONITOR_SHOW_WIDGET request attribute that overrides the widget visibility.
			// That way the application can decide whether a particular user is allowed to see the widget.P
			securityFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/stagemonitor/*");
			securityFilter.setAsyncSupported(true);

			final FilterRegistration.Dynamic monitorFilter = ctx.addFilter(HttpRequestMonitorFilter.class.getSimpleName(), new HttpRequestMonitorFilter());
			monitorFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
			monitorFilter.setAsyncSupported(true);

			try {
				ctx.addListener(SessionCounter.class);
			} catch (IllegalArgumentException e) {
				// embedded servlet containers like jetty don't necessarily support sessions
			}

			final ServletRegistration.Dynamic healthServlet = ctx.addServlet(HealthCheckServlet.class.getSimpleName(), new HealthCheckServlet(Stagemonitor.getHealthCheckRegistry()));
			healthServlet.addMapping("/stagemonitor/status");
			healthServlet.setAsyncSupported(true);
		}

	}
}
