package org.stagemonitor.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.util.Collections.emptySet;

public class Configuration {

	public static final String STAGEMONITOR_PASSWORD = "stagemonitor.password";
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private ConcurrentMap<String, Object> propertiesCache = new ConcurrentHashMap<String, Object>();
	private List<ConfigurationSource> configurationSources = new LinkedList<ConfigurationSource>();

	public Configuration() {
		configurationSources.add(new SystemPropertyConfigurationSource());
		configurationSources.add(new PropertyFileConfigurationSource());
	}

	public void reload() {
		for (ConfigurationSource configurationSource : configurationSources) {
			configurationSource.reload();
		}
		propertiesCache.clear();
	}

	public void addConfigurationSource(ConfigurationSource configurationSource, boolean firstPrio) {
		if (configurationSource == null) {
			return;
		}
		if (firstPrio) {
			configurationSources.add(0, configurationSource);
		} else {
			configurationSources.add(configurationSource);
		}
	}

	public boolean isStagemonitorActive() {
		return getBoolean("stagemonitor.active", true);
	}

	/**
	 * Specifies the minimum number of requests that have to be issued against the application before metrics are
	 * collected.
	 *
	 * @return the number of warmup requests
	 */
	public int getNoOfWarmupRequests() {
		return getInt("stagemonitor.requestmonitor.noOfWarmupRequests", 0);
	}

	/**
	 * A timespan in seconds after the start of the server where no metrics are collected.
	 *
	 * @return the warmups in seconds
	 */
	public int getWarmupSeconds() {
		return getInt("stagemonitor.requestmonitor.warmupSeconds", 0);
	}

	/**
	 * Whether or not metrics about requests (Call Stacks, response times, errors status codes) should be collected.
	 *
	 * @return <code>true</code> if metrics about requests should be collected, <code>false</code> otherwise
	 */
	public boolean isCollectRequestStats() {
		return getBoolean("stagemonitor.requestmonitor.collectRequestStats", true);
	}

	/**
	 * Whether or not a {@link com.codahale.metrics.Timer} for the cpu time of executions should be created.
	 *
	 * @return <code>true</code> if metrics about the cpu time of executions should be collected, <code>false</code>
	 * otherwise
	 */
	public boolean isCollectCpuTime() {
		return getBoolean("stagemonitor.requestmonitor.cpuTime", false);
	}

	/**
	 * Whether or not requests should be ignored, if they will not be handled by a Spring MVC controller method.
	 * <p/>
	 * This is handy, if you are not interested in the performance of serving static files.
	 * Setting this to <code>true</code> can also significantly reduce the amount of files (and thus storing space)
	 * Graphite will allocate.
	 *
	 * @return true, if non Spring MVC requests should be ignored, false otherwise
	 */
	public boolean isMonitorOnlySpringMvcRequests() {
		return getBoolean("stagemonitor.requestmonitor.spring.monitorOnlySpringMvcRequests", false);
	}

	/**
	 * Whether or not HTTP headers should be collected with a call stack.
	 *
	 * @return <code>true</code> if HTTP headers should be collected, <code>false</code> otherwise
	 */
	public boolean isCollectHeaders() {
		return getBoolean("stagemonitor.requestmonitor.http.collectHeaders", true);
	}

	/**
	 * Whether or not the user-agent header should be parsed and analyzed to get information about the browser,
	 * device type and operating system.
	 *
	 * @return true, if user-agent header should be parsed, false otherwise
	 */
	public boolean isParseUserAgent() {
		return getBoolean("stagemonitor.requestmonitor.http.parseUserAgent", true);
	}

	/**
	 * A list of (non case sensitive) header names that should not be collected.
	 *
	 * @return list header names not to collect
	 */
	public Collection<String> getExcludedHeaders() {
		return getLowerStrings("stagemonitor.requestmonitor.http.headers.excluded", "cookie,Authorization");
	}

	/**
	 * A list of request parameter name patterns that should not be collected.
	 * <p/>
	 * A request parameter is either a query string or a application/x-www-form-urlencoded request body
	 * (POST form content)
	 *
	 * @return list of confidential request parameter names
	 */
	public Collection<Pattern> getConfidentialRequestParams() {
		return getPatterns("stagemonitor.requestmonitor.http.requestparams.confidential.regex",
				"(?i).*pass.*, (?i).*credit.*, (?i).*pwd.*");
	}

	/**
	 * The amount of time between console reports (in seconds).
	 * <p>
	 * To deactivate console reports, set this to a value below 1.
	 * </p>
	 *
	 * @return the amount of time between console reports in seconds
	 */
	public long getConsoleReportingInterval() {
		return getLong("stagemonitor.reporting.interval.console", 60L);
	}

	/**
	 * Whether or not to expose all metrics as MBeans.
	 *
	 * @return <code>true</code>, if all metrics should be exposed as MBeans, <code>false</code> otherwise
	 */
	public boolean reportToJMX() {
		return getBoolean("stagemonitor.reporting.jmx", true);
	}

	/**
	 * The amount of time between the metrics are reported to graphite (in seconds).
	 * <p>
	 * To deactivate graphite reporting, set this to a value below 1, or don't provide
	 * stagemonitor.reporting.graphite.hostName.
	 * </p>
	 *
	 * @return the amount of time between graphite reports in seconds
	 */
	public long getGraphiteReportingInterval() {
		return getLong("stagemonitor.reporting.interval.graphite", 60);
	}

	/**
	 * The name of the host where graphite is running
	 * <p><b>This setting is mandatory, if you want to use the dashboard UI.</b></p>
	 *
	 * @return graphite's host's name
	 */
	public String getGraphiteHostName() {
		return getString("stagemonitor.reporting.graphite.hostName");
	}

	/**
	 * The port where carbon is listening.
	 *
	 * @return the graphite carbon port
	 */
	public int getGraphitePort() {
		return getInt("stagemonitor.reporting.graphite.port", 2003);
	}

	/**
	 * The minimal inclusive execution time of a method before it is included in a call stack.
	 *
	 * @return the minimal execution time
	 */
	public long getMinExecutionTimeNanos() {
		return getLong("stagemonitor.profiler.minExecutionTimeNanos", 100000L);
	}

	/**
	 * Defines after how many requests to a URL group a call stack should be collected.
	 *
	 * @return the number of requests to a URL group after a call stack should be collected
	 */
	public int getCallStackEveryXRequestsToGroup() {
		return getInt("stagemonitor.profiler.callStackEveryXRequestsToGroup", 1);
	}

	/**
	 * Whether or not call stacks should be logged.
	 *
	 * @return <code>true</code>, if call stacks should be logged, <code>false</code> otherwise
	 */
	public boolean isLogCallStacks() {
		return getBoolean("stagemonitor.profiler.logCallStacks", true);
	}

	/**
	 * Whether or not sql statements should be included in the call stack.
	 *
	 * @return <code>true</code>, if sql statements should be included in the call stack, <code>false</code> otherwise
	 */
	public boolean collectSql() {
		return getBoolean("stagemonitor.profiler.jdbc.collectSql", true);
	}

	/**
	 * Whether or not the prepared statement placeholders (?) should be replaced with the actual parameters.
	 * <p/>
	 * Only applies, if {@link #collectSql()} is true.
	 *
	 * @return <code>true</code>, if parameters should be collected, <code>false</code> otherwise
	 */
	public boolean collectPreparedStatementParameters() {
		return getBoolean("stagemonitor.profiler.jdbc.collectPreparedStatementParameters", false);
	}

	/**
	 * When set, call stacks will be deleted automatically after the specified interval
	 * <p/>
	 * In case you do not specify a time unit like d (days), m (minutes), h (hours), ms (milliseconds) or w (weeks),
	 * milliseconds is used as default unit.
	 *
	 * @return the time to live interval for a call stack
	 */
	public String getCallStacksTimeToLive() {
		return getString("stagemonitor.requestmonitor.requestTraceTTL", "1w");
	}

	/**
	 * Whether or not db execution time should be collected per request
	 * <p/>
	 * If set to true, a timer will be created for each request to record the total db time per request.
	 *
	 * @return <code>true</code>, if db execution time should be collected per request, <code>false</code> otherwise
	 */
	public boolean collectDbTimePerRequest() {
		return getBoolean("stagemonitor.jdbc.collectDbTimePerRequest", false);
	}

	/**
	 * The name of the application
	 * <p>
	 * <b>Either this property or the <code>display-name</code> in <code>web.xml</code> is mandatory!</b>
	 * </p>
	 *
	 * @return the application name
	 */
	public String getApplicationName() {
		return getString("stagemonitor.applicationName");
	}

	/**
	 * The instance name.
	 * <p>If this property is not set, the instance name set to the first request's
	 * {@link javax.servlet.ServletRequest#getServerName()}<br/>
	 * <b>that means that the collection of metrics does not start before the first request is executed</b>
	 * </p>
	 *
	 * @return
	 */
	public String getInstanceName() {
		return getString("stagemonitor.instanceName");
	}

	/**
	 * The URL of the elasticsearch server that stores the call stacks.
	 * If the URL is not provided, the call stacks won't get stored.
	 *
	 * @return the server url
	 */
	public String getElasticsearchUrl() {
		final String url = getString("stagemonitor.elasticsearch.url");
		if (url != null && url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}

	/**
	 * A comma separated list of metric names that should not be collected.
	 *
	 * @return a pattern list of excluded metric names
	 */
	public Collection<Pattern> getExcludedMetricsPatterns() {
		return getPatterns("stagemonitor.metrics.excluded.pattern", "");
	}

	/**
	 * A comma separated list of plugin names (the simple class name) that should not be active.
	 *
	 * @return the disabled plugin names
	 */
	public Collection<String> getDisabledPlugins() {
		return getStrings("stagemonitor.plugins.disabled", "");
	}

	/**
	 * Combine url paths by regex to a single url group.
	 * <p>
	 * E.g. <code>(.*).js: *.js</code> combines all URLs that end with .js to a group named *.js.
	 * The metrics for all URLs matching the pattern are consolidated and shown in one row in the request table.
	 * </p>
	 * <p>
	 * The syntax is <code>&lt;regex>: &lt;group name>[, &lt;regex>: &lt;group name>]*</code>
	 * </p>
	 *
	 * @return the url groups definition
	 */
	public Map<Pattern, String> getGroupUrls() {
		return getPatternMap("stagemonitor.groupUrls",
						"(.*).js$:   *.js," +
						"(.*).css$:  *.css," +
						"(.*).jpg$:  *.jpg," +
						"(.*).jpeg$: *.jpeg," +
						"(.*).png$:  *.png");
	}

	/**
	 * The name of the ehcache to instrument
	 * <p/>
	 * (the value of the 'name' attribute of the 'ehcache' tag in ehcache.xml)
	 *
	 * @return the name of the ehcache to instrument
	 */
	public String getEhCacheName() {
		return getString("stagemonitor.ehcache.name", null);
	}

	/**
	 * The password that is required to dynamically update the configuration via a query parameter.
	 * <p/>
	 * If not set (default) configuration reloading is disabled. If set, you have to include the query parameter
	 * <code>stagemonitor.password=&lt;password></code>, if you want to dynamically update the
	 * configuration via query parameters. If set to an empty string, the password is not required.
	 *
	 * @return the password that is required to dynamically update the configuration via a query parameter. Returns
	 * <code>null</code> if not set.
	 */
	public String getConfigurationUpdatePassword() {
		return getString(STAGEMONITOR_PASSWORD, null);
	}

	/**
	 * If active, stagemonitor will collect internal performance data
	 *
	 * @return <code>true</code>, if internal performance data should be collected, false otherwise
	 */
	public boolean isInternalMonitoringActive() {
		return getBoolean("stagemonitor.internal.monitoring", false);
	}

	/**
	 * If active, stagemonitor will inject a widget in the web site containing the calltrace metrics.
	 * Requires Servlet-Api >= 3.0
	 *
	 * @return <code>true</code>, if the widget shall be injected, false otherwise
	 */
	public boolean isStagemonitorWidgetEnabled() {
		return getBoolean("stagemonitor.web.widget.enabled", false);
	}

	public String getString(final String key) {
		return getString(key, null);
	}

	public String getString(final String key, final String defaultValue) {
		return getAndCache(key, null, new PropertyLoader<String>() {
			@Override
			public String load() {
				return getTrimmedProperty(key, defaultValue);
			}
		});
	}

	private String getTrimmedProperty(String key, String defaultValue) {
		String property = null;
		for (ConfigurationSource configurationSource : configurationSources) {
			property = configurationSource.getValue(key);
			if (property != null) {
				break;
			}
		}
		if (property != null) {
			return property.trim();
		} else {
			return defaultValue;
		}
	}

	public Collection<String> getLowerStrings(final String key, final String defaultValue) {
		return getAndCache(key, null, new PropertyLoader<Collection<String>>() {
			@Override
			public Collection<String> load() {
				String property = getTrimmedProperty(key, defaultValue);
				if (property != null && property.length() > 0) {
					final String[] split = property.split(",");
					for (int i = 0; i < split.length; i++) {
						split[i] = split[i].trim().toLowerCase();
					}
					return new LinkedHashSet<String>(Arrays.asList(split));
				}
				return emptySet();
			}
		});
	}

	public Collection<Pattern> getPatterns(final String key, final String defaultValue) {
		final Collection<String> strings = getStrings(key, defaultValue);
		Collection<Pattern> patterns = new LinkedHashSet<Pattern>((int) Math.ceil(strings.size() / 0.75));
		for (String patternString : strings) {
			try {
				patterns.add(Pattern.compile(patternString));
			} catch (PatternSyntaxException e) {
				logger.warn("Error while compiling pattern " + patternString + " (this exception is ignored)", e);
			}
		}
		return patterns;
	}

	public Collection<String> getStrings(final String key, final String defaultValue) {
		return getAndCache(key, null, new PropertyLoader<Collection<String>>() {
			@Override
			public Collection<String> load() {
				String property = getTrimmedProperty(key, defaultValue);
				if (property != null && property.length() > 0) {
					final String[] split = property.split(",");
					for (int i = 0; i < split.length; i++) {
						split[i] = split[i].trim();
					}
					return new LinkedHashSet<String>(Arrays.asList(split));
				}
				return emptySet();
			}
		});
	}

	public boolean getBoolean(final String key, final boolean defaultValue) {
		return getAndCache(key, defaultValue, new PropertyLoader<Boolean>() {
			@Override
			public Boolean load() {
				return Boolean.parseBoolean(getTrimmedProperty(key, Boolean.toString(defaultValue)));
			}
		});
	}

	public int getInt(String key, int defaultValue) {
		return (int) getLong(key, defaultValue);
	}

	public long getLong(final String key, final long defaultValue) {
		return getAndCache(key, defaultValue, new PropertyLoader<Long>() {
			@Override
			public Long load() {
				String value = getTrimmedProperty(key, Long.toString(defaultValue));
				try {
					return Long.parseLong(value);
				} catch (NumberFormatException e) {
					logger.warn(e.getMessage() + " (this exception is ignored)", e);
					return defaultValue;
				}
			}
		});
	}

	public Map<Pattern, String> getPatternMap(final String key, final String defaultValue) {
		return getAndCache(key, null, new PropertyLoader<Map<Pattern, String>>() {
			@Override
			public Map<Pattern, String> load() {
				String patternString = getTrimmedProperty(key, defaultValue);
				try {
					String[] groups = patternString.split(",");
					Map<Pattern, String> pattenGroupMap = new HashMap<Pattern, String>(groups.length);

					for (String group : groups) {
						group = group.trim();
						String[] keyValue = group.split(":");
						if (keyValue.length != 2) {
							throw new IllegalArgumentException();
						}
						pattenGroupMap.put(Pattern.compile(keyValue[0].trim()), keyValue[1].trim());
					}
					return pattenGroupMap;
				} catch (RuntimeException e) {
					logger.warn("Error while parsing pattern map. Expected format <regex>: <name>[, <regex>: <name>]. "
							+ "Actual value: " + patternString + " (this exception is ignored)", e);
					return Collections.emptyMap();
				}
			}
		});
	}

	private <T> T getAndCache(String key, T defaultValue, PropertyLoader<T> propertyLoader) {
		@SuppressWarnings("unchecked")
		T result = (T) propertiesCache.get(key);
		if (result == null) {
			result = propertyLoader.load();
			if (result == null) {
				result = defaultValue;
			}
			if (result != null) {
				propertiesCache.put(key, result);
			}
		}
		return result;
	}

	private interface PropertyLoader<T> {
		T load();
	}

}
