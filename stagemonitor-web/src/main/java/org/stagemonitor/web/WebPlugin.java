package org.stagemonitor.web;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.ConfigurationOption;
import org.stagemonitor.core.StageMonitorPlugin;
import org.stagemonitor.core.pool.MBeanPooledResourceImpl;
import org.stagemonitor.core.pool.PooledResourceMetricsRegisterer;
import org.stagemonitor.core.rest.RestClient;

import java.util.ArrayList;
import java.util.List;

public class WebPlugin implements StageMonitorPlugin {

	public static final String METRIC_PREFIX = "server.threadpool";
	public static final String HTTP_COLLECT_HEADERS = "stagemonitor.requestmonitor.http.collectHeaders";
	public static final String HTTP_PARSE_USER_AGENT = "stagemonitor.requestmonitor.http.parseUserAgent";
	public static final String HTTP_HEADERS_EXCLUDED = "stagemonitor.requestmonitor.http.headers.excluded";
	public static final String HTTP_REQUESTPARAMS_CONFIDENTIAL_REGEX = "stagemonitor.requestmonitor.http.requestparams.confidential.regex";
	public static final String WIDGET_ENABLED = "stagemonitor.web.widget.enabled";
	public static final String STAGEMONITOR_PASSWORD = "stagemonitor.password";
	public static final String GROUP_URLS = "stagemonitor.groupUrls";
	private final Logger logger = LoggerFactory.getLogger(getClass());

	boolean requiredPropertiesSet = true;

	@Override
	public List<ConfigurationOption> getConfigurationOptions() {
		List<ConfigurationOption> config = new ArrayList<ConfigurationOption>();
		config.add(ConfigurationOption.builder()
				.key(HTTP_COLLECT_HEADERS)
				.dynamic(true)
				.label("Collect HTTP headers")
				.description("Whether or not HTTP headers should be collected with a call stack.")
				.defaultValue("true")
				.build());
		config.add(ConfigurationOption.builder()
				.key(HTTP_PARSE_USER_AGENT)
				.dynamic(true)
				.label("Analyze user agent")
				.description("Whether or not the user-agent header should be parsed and analyzed to get information " +
						"about the browser, device type and operating system.")
				.defaultValue("true")
				.build());
		config.add(ConfigurationOption.builder()
				.key(HTTP_HEADERS_EXCLUDED)
				.dynamic(true)
				.label("Do not collect headers")
				.description("A list of (case insensitive) header names that should not be collected.")
				.defaultValue("cookie,Authorization")
				.build());
		config.add(ConfigurationOption.builder()
				.key(HTTP_REQUESTPARAMS_CONFIDENTIAL_REGEX)
				.dynamic(true)
				.label("Confidential request parameters (regex)")
				.description("A list of request parameter name patterns that should not be collected.\n" +
						"A request parameter is either a query string or a application/x-www-form-urlencoded request " +
						"body (POST form content)")
				.defaultValue("(?i).*pass.*, (?i).*credit.*, (?i).*pwd.*")
				.build());
		config.add(ConfigurationOption.builder()
				.key(WIDGET_ENABLED)
				.dynamic(true)
				.label("In browser widget enabled")
				.description("If active, stagemonitor will inject a widget in the web site containing the calltrace " +
						"metrics.\n" +
						"Requires Servlet-Api >= 3.0")
				.defaultValue("false")
				.build());
		config.add(ConfigurationOption.builder()
				.key(STAGEMONITOR_PASSWORD)
				.dynamic(false)
				.label("Password for dynamic configuration changes")
				.description("The password that is required to dynamically update the configuration via the configuration endpoint.\n" +
						"If not set (default) configuration reloading is disabled. If set, you have to include " +
						"parameter stagemonitor.password=password, if you want to dynamically update the " +
						"configuration. If set to an empty string, the password parameter is not required.\n" +
						"Requires Servlet-Api >= 3.0")
				.defaultValue("false")
				.build());
		config.add(ConfigurationOption.builder()
				.key(GROUP_URLS)
				.dynamic(true)
				.label("Group URLs regex")
				.description("Combine url paths by regex to a single url group.\n" +
						"E.g. '(.*).js: *.js' combines all URLs that end with .js to a group named *.js. " +
						"The metrics for all URLs matching the pattern are consolidated and shown in one row in the request table. " +
						"The syntax is '<regex>: <group name>[, <regex>: <group name>]*'")
				.defaultValue("(.*).js$:   *.js," +
						"(.*).css$:  *.css," +
						"(.*).jpg$:  *.jpg," +
						"(.*).jpeg$: *.jpeg," +
						"(.*).png$:  *.png")
				.build());
		return config;
	}

	@Override
	public void initializePlugin(MetricRegistry registry, Configuration conf) {
		monitorServerThreadPool(registry, conf);
		RestClient.sendGrafanaDashboardAsync(conf.getElasticsearchUrl(), "Server.json");
		RestClient.sendGrafanaDashboardAsync(conf.getElasticsearchUrl(), "KPIs over Time.json");
	}

	private void monitorServerThreadPool(MetricRegistry registry, Configuration conf) {
		final String objectName = getRequeredProperty("stagemonitor.server.threadpool.objectName", conf);
		final String mbeanKeyPropertyName = getRequeredProperty("stagemonitor.server.threadpool.mbeanKeyPropertyName", conf);
		final String mbeanActiveAttribute = getRequeredProperty("stagemonitor.server.threadpool.mbeanActiveAttribute", conf);
		final String mbeanCountAttribute = getRequeredProperty("stagemonitor.server.threadpool.mbeanCountAttribute", conf);
		final String mbeanMaxAttribute = getRequeredProperty("stagemonitor.server.threadpool.mbeanMaxAttribute", conf);
		final String mbeanQueueAttribute = conf.getString("stagemonitor.server.threadpool.mbeanQueueAttribute");
		if (requiredPropertiesSet) {
			final List<MBeanPooledResourceImpl> pools = MBeanPooledResourceImpl.of(objectName,
					METRIC_PREFIX, mbeanKeyPropertyName, mbeanActiveAttribute, mbeanCountAttribute,
					mbeanMaxAttribute, mbeanQueueAttribute);
			PooledResourceMetricsRegisterer.registerPooledResources(pools, registry);
		}
	}

	private String getRequeredProperty(String propertyKey, Configuration conf) {
		String requredProperty = conf.getString(propertyKey);
		if (requredProperty == null || requredProperty.isEmpty()) {
			logger.info(propertyKey + " is empty, Server Plugin deactivated");
			requiredPropertiesSet = false;
		}
		return requredProperty;
	}
}
