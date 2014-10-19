package org.stagemonitor.web;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.pool.MBeanPooledResourceImpl;
import org.stagemonitor.core.pool.PooledResourceMetricsRegisterer;
import org.stagemonitor.core.rest.RestClient;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class WebPlugin implements StagemonitorPlugin {

	public static final String WEB_PLUGIN = "Web Plugin";
	private final Logger logger = LoggerFactory.getLogger(getClass());
	boolean requiredPropertiesSet = true;
	private final ConfigurationOption<List<Pattern>> requestParamsConfidential = ConfigurationOption.regexListOption()
			.key("stagemonitor.requestmonitor.http.requestparams.confidential.regex")
			.dynamic(true)
			.label("Confidential request parameters (regex)")
			.description("A list of request parameter name patterns that should not be collected.\n" +
					"A request parameter is either a query string or a application/x-www-form-urlencoded request " +
					"body (POST form content)")
			.defaultValue(Arrays.asList(
					Pattern.compile("(?i).*pass.*"),
					Pattern.compile("(?i).*credit.*"),
					Pattern.compile("(?i).*pwd.*")))
			.pluginName(WEB_PLUGIN)
			.build();
	private ConfigurationOption<Boolean> collectHttpHeaders = ConfigurationOption.booleanOption()
			.key("stagemonitor.requestmonitor.http.collectHeaders")
			.dynamic(true)
			.label("Collect HTTP headers")
			.description("Whether or not HTTP headers should be collected with a call stack.")
			.defaultValue(true)
			.pluginName(WEB_PLUGIN)
			.build();
	private ConfigurationOption<Boolean> parseUserAgent = ConfigurationOption.booleanOption()
			.key("stagemonitor.requestmonitor.http.parseUserAgent")
			.dynamic(true)
			.label("Analyze user agent")
			.description("Whether or not the user-agent header should be parsed and analyzed to get information " +
					"about the browser, device type and operating system.")
			.defaultValue(true)
			.pluginName(WEB_PLUGIN)
			.build();
	private ConfigurationOption<Collection<String>> excludeHeaders = ConfigurationOption.lowerStringsOption()
			.key("stagemonitor.requestmonitor.http.headers.excluded")
			.dynamic(true)
			.label("Do not collect headers")
			.description("A list of (case insensitive) header names that should not be collected.")
			.defaultValue(new LinkedHashSet<String>(Arrays.asList("cookie", "authorization")))
			.pluginName(WEB_PLUGIN)
			.build();
	private final ConfigurationOption<Boolean> widgetEnabled = ConfigurationOption.booleanOption()
			.key("stagemonitor.web.widget.enabled")
			.dynamic(true)
			.label("In browser widget enabled")
			.description("If active, stagemonitor will inject a widget in the web site containing the calltrace " +
					"metrics.\n" +
					"Requires Servlet-Api >= 3.0")
			.defaultValue(false)
			.pluginName(WEB_PLUGIN)
			.build();
	private final ConfigurationOption<Map<Pattern, String>> groupUrls = ConfigurationOption.regexMapOption()
			.key("stagemonitor.groupUrls")
			.dynamic(true)
			.label("Group URLs regex")
			.description("Combine url paths by regex to a single url group.\n" +
					"E.g. '(.*).js: *.js' combines all URLs that end with .js to a group named *.js. " +
					"The metrics for all URLs matching the pattern are consolidated and shown in one row in the request table. " +
					"The syntax is '<regex>: <group name>[, <regex>: <group name>]*'")
			.defaultValue(
					new LinkedHashMap<Pattern, String>() {{
						put(Pattern.compile("(.*).js$"), "*.js");
						put(Pattern.compile("(.*).css$"), "*.css");
						put(Pattern.compile("(.*).jpg$"), "*.jpg");
						put(Pattern.compile("(.*).jpeg$"), "*.jpeg");
						put(Pattern.compile("(.*).png$"), "*.png");
					}})
			.pluginName(WEB_PLUGIN)
			.build();
	// TODO descriptions
	private final ConfigurationOption<String> serverThreadPoolObjectName = ConfigurationOption.stringOption()
			.key("stagemonitor.server.threadpool.objectName")
			.label("Server Thread Pool MBean Object Name")
			.description("")
			.pluginName(WEB_PLUGIN)
			.build(); 
	private final ConfigurationOption<String> serverThreadPoolMBeanPropertyName = ConfigurationOption.stringOption()
			.key("stagemonitor.server.threadpool.mbeanKeyPropertyName")
			.label("Server Thread Pool MBean Property Name")
			.description("")
			.pluginName(WEB_PLUGIN)
			.build(); 
	private final ConfigurationOption<String> serverThreadPoolMBeanActiveAttribute = ConfigurationOption.stringOption()
			.key("stagemonitor.server.threadpool.mbeanActiveAttribute")
			.label("Server Thread Pool MBean Active Attribute")
			.description("")
			.pluginName(WEB_PLUGIN)
			.build(); 
	private final ConfigurationOption<String> serverThreadPoolMBeanCountAttribute = ConfigurationOption.stringOption()
			.key("stagemonitor.server.threadpool.mbeanCountAttribute")
			.label("Server Thread Pool MBean Count Attribute")
			.description("")
			.pluginName(WEB_PLUGIN)
			.build(); 
	private final ConfigurationOption<String> serverThreadPoolMBeanMaxAttribute = ConfigurationOption.stringOption()
			.key("stagemonitor.server.threadpool.mbeanMaxAttribute")
			.label("Server Thread Pool MBean Max Attribute")
			.description("")
			.pluginName(WEB_PLUGIN)
			.build(); 
	private final ConfigurationOption<String> serverThreadPoolMBeanQueueAttribute = ConfigurationOption.stringOption()
			.key("stagemonitor.server.threadpool.mbeanQueueAttribute")
			.label("Server Thread Pool MBean Queue Attribute")
			.description("")
			.pluginName(WEB_PLUGIN)
			.build();
	private final ConfigurationOption<Boolean> rumEnabled = ConfigurationOption.booleanOption()
			.key("stagemonitor.web.rum.enabled")
			.dynamic(true)
			.label("Enable Real User Monitoring")
			.description("The Real User Monitoring feature collects the browser, network and overall percieved " +
					"execution time from the user's perspective. When activated, a piece of javascript will be " +
					"injected to each html page that collects the data from real users and sends it back " +
					"to the server. Servlet API 3.0 or higher is required for this.")
			.defaultValue(true)
			.pluginName(WEB_PLUGIN)
			.build();
	private final ConfigurationOption<Boolean> collectPageLoadTimesPerRequest = ConfigurationOption.booleanOption()
			.key("stagemonitor.web.collectPageLoadTimesPerRequest")
			.dynamic(true)
			.label("Collect Page Load Time data per request group")
			.description("Whether or not browser, network and overall execution time should be collected per request group.\n" +
					"If set to true, four additional timers will be created for each request group to record the page " +
					"rendering time, dom processing time, network time and overall time per request. " +
					"If set to false, the times of all requests will be aggregated.")
			.defaultValue(false)
			.pluginName(WEB_PLUGIN)
			.build();
	private final ConfigurationOption<Collection<String>> excludedRequestPaths = ConfigurationOption.stringsOption()
			.key("stagemonitor.web.paths.excluded")
			.dynamic(false)
			.label("Excluded paths")
			.description("Request paths that should not be monitored. " +
					"A value of '/aaa' means, that all paths starting with '/aaa' should not be monitored." +
					" It's recommended to not monitor static resources, as they are typically not interesting to " +
					"monitor but consume resources when you do.")
			.defaultValue(Collections.<String>emptyList())
			.pluginName(WEB_PLUGIN)
			.build();
	private final ConfigurationOption<Boolean> monitorOnlyForwardedRequests = ConfigurationOption.booleanOption()
			.key("stagemonitor.web.monitorOnlyForwardedRequests")
			.dynamic(true)
			.label("Monitor only forwarded requests")
			.description("Sometimes you only want to monitor forwarded requests, for example if you have a rewrite " +
					"filter that translates a external URI (/a) to a internal URI (/b). If only /b should be monitored," +
					"set the value to true.")
			.defaultValue(false)
			.pluginName(WEB_PLUGIN)
			.build();

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		return Arrays.<ConfigurationOption<?>>asList(collectHttpHeaders, parseUserAgent, excludeHeaders, 
				requestParamsConfidential, widgetEnabled, groupUrls, rumEnabled, collectPageLoadTimesPerRequest,
				excludedRequestPaths, monitorOnlyForwardedRequests);
	}

	@Override
	public void initializePlugin(MetricRegistry registry, Configuration config) {
		final CorePlugin corePlugin = config.getConfig(CorePlugin.class);
		monitorServerThreadPool(registry, config.getConfig(WebPlugin.class));
		RestClient.sendGrafanaDashboardAsync(corePlugin.getElasticsearchUrl(), "Server.json");
		RestClient.sendGrafanaDashboardAsync(corePlugin.getElasticsearchUrl(), "KPIs over Time.json");
	}

	private void monitorServerThreadPool(MetricRegistry registry, WebPlugin webPlugin) {
		final String objectName = getRequeredProperty(webPlugin.serverThreadPoolObjectName);
		final String mbeanKeyPropertyName = getRequeredProperty(webPlugin.serverThreadPoolMBeanPropertyName);
		final String mbeanActiveAttribute = getRequeredProperty(webPlugin.serverThreadPoolMBeanActiveAttribute);
		final String mbeanCountAttribute = getRequeredProperty(webPlugin.serverThreadPoolMBeanCountAttribute);
		final String mbeanMaxAttribute = getRequeredProperty(webPlugin.serverThreadPoolMBeanMaxAttribute);
		final String mbeanQueueAttribute = webPlugin.serverThreadPoolMBeanQueueAttribute.getValue();
		if (requiredPropertiesSet) {
			final List<MBeanPooledResourceImpl> pools = MBeanPooledResourceImpl.of(objectName,
					"server.threadpool", mbeanKeyPropertyName, mbeanActiveAttribute, mbeanCountAttribute,
					mbeanMaxAttribute, mbeanQueueAttribute);
			PooledResourceMetricsRegisterer.registerPooledResources(pools, registry);
		}
	}

	private String getRequeredProperty(ConfigurationOption<String> option) {
		String requredProperty = option.getValue();
		if (requredProperty == null || requredProperty.isEmpty()) {
			logger.info(option.getKey() + " is empty, Server Plugin deactivated");
			requiredPropertiesSet = false;
		}
		return requredProperty;
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

	public List<Pattern> getRequestParamsConfidential() {
		return requestParamsConfidential.getValue();
	}

	public boolean isRealUserMonitoringEnabled() {
		return rumEnabled.getValue();
	}

	public boolean isCollectPageLoadTimesPerRequest() {
		return collectPageLoadTimesPerRequest.getValue();
	}

	public Collection<String> getExcludedRequestPaths() {
		return excludedRequestPaths.getValue();
	}

	public boolean isMonitorOnlyForwardedRequests() {
		return monitorOnlyForwardedRequests.getValue();
	}
}
