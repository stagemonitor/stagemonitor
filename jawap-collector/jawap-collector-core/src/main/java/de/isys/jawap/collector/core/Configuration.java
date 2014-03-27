package de.isys.jawap.collector.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.util.Collections.emptyList;

public class Configuration {

	private final Log logger = LogFactory.getLog(getClass());
	private Properties properties;
	private ConcurrentMap<String, Object> propertiesCache = new ConcurrentHashMap<String, Object>();

	public Configuration() {
		loadProperties();
	}

	/**
	 * Specifies the minimum number of requests that have to be issued against the application before metrics are collected.
	 *
	 * @return the number of warmup requests
	 */
	public int getNoOfWarmupRequests() {
		return getInt("jawap.monitor.noOfWarmupRequests", 0);
	}

	/**
	 * A timespan in seconds after the start of the server where no metrics are collected.
	 *
	 * @return the warmups in seconds
	 */
	public int getWarmupSeconds() {
		return getInt("jawap.monitor.warmupSeconds", 0);
	}

	/**
	 * Whether or not metrics about requests (Call Stacks, response times, errors status codes) should be collected.
	 *
	 * @return <code>true</code> if metrics about requests should be collected, <code>false</code> otherwise
	 */
	public boolean isCollectRequestStats() {
		return getBoolean("jawap.monitor.collectRequestStats", true);
	}

	/**
	 * Whether or not HTTP headers should be collected with a call stack.
	 *
	 * @return <code>true</code> if HTTP headers should be collected, <code>false</code> otherwise
	 */
	public boolean isCollectHeaders() {
		return getBoolean("jawap.monitor.http.collectHeaders", true);
	}

	/**
	 * A list of header names that should not be collected.
	 *
	 * @return list header names not to collect
	 */
	public List<String> getExcludedHeaders() {
		return getLowerStrings("jawap.monitor.http.headers.excluded", "cookie");
	}

	/**
	 * A list of query parameter name patterns that should not be collected.
	 *
	 * @return list of confidential query parameter names
	 */
	public List<Pattern> getConfidentialQueryParams() {
		return getPatterns("jawap.monitor.http.queryparams.confidential.regex", "(?i).*pass.*, (?i).*credit.*, (?i).*pwd.*");
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
		return getLong("jawap.reporting.interval.console", 60);
	}

	/**
	 * Whether or not to expose all metrics as MBeans.
	 *
	 * @return <code>true</code>, if all metrics should be exposed as MBeans, <code>false</code> otherwise
	 */
	public boolean reportToJMX() {
		return getBoolean("jawap.reporting.jmx", true);
	}

	/**
	 * The amount of time between the metrics are reported to graphite (in seconds).
	 * <p>
	 * To deactivate graphite reporting, set this to a value below 1, or don't provide jawap.reporting.graphite.hostName.
	 * </p>
	 *
	 * @return the amount of time between graphite reports in seconds
	 */
	public long getGraphiteReportingInterval() {
		return getLong("jawap.reporting.interval.graphite", 60);
	}

	/**
	 * The name of the host where graphite is running
	 * <p><b>This setting is mandatory, if you want to use the dashboard UI.</b></p>
	 *
	 * @return graphite's host's name
	 */
	public String getGraphiteHostName() {
		return getString("jawap.reporting.graphite.hostName");
	}

	/**
	 * The port where carbon is listening.
	 *
	 * @return the graphite carbon port
	 */
	public int getGraphitePort() {
		return getInt("jawap.reporting.graphite.port", 2003);
	}

	/**
	 * The minimal inclusive execution time of a method before it is included in a call stack.
	 *
	 * @return the minimal execution time
	 */
	public long getMinExecutionTimeNanos() {
		return getLong("jawap.profiler.minExecutionTimeNanos", 100000L);
	}

	/**
	 * Defines after how many requests to a URL group a call stack should be collected.
	 *
	 * @return the number of requests to a URL group after a call stack should be collected
	 */
	public int getCallStackEveryXRequestsToGroup() {
		return getInt("jawap.profiler.callStackEveryXRequestsToGroup", 1);
	}

	/**
	 * Whether or not call stacks should be logged.
	 *
	 * @return <code>true</code>, if call stacks should be logged, <code>false</code> otherwise
	 */
	public boolean isLogCallStacks() {
		return getBoolean("jawap.profiler.logCallStacks", true);
	}

	/**
	 * Whether or not call stacks should reported to the server.
	 *
	 * @return <code>true</code>, if call stacks should reported to the server, <code>false</code> otherwise
	 */
	public boolean isReportCallStacksToServer() {
		return getBoolean("jawap.profiler.reportCallStacksToServer", true);
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
		return getString("jawap.applicationName");
	}

	/**
	 * The instance name.
	 * <p>If this property is not set, the instance name set to the first request's {@link javax.servlet.ServletRequest#getServerName()}<br/>
	 * <b>that means that the collection of metrics does not start before the first request is executed</b>
	 * </p>
	 *
	 * @return
	 */
	public String getInstanceName() {
		return getString("jawap.instanceName");
	}

	/**
	 * The URL of the jawap server
	 * <p/>
	 * <b>This setting is mandatory if jawap.profiler.reportCallStacksToServer is set to true</b>
	 *
	 * @return the server url
	 */
	public String getServerUrl() {
		return getString("jawap.serverUrl");
	}

	/**
	 * A comma separated list of metric names that should not be collected.
	 *
	 * @return a pattern list of excluded metric names
	 */
	public List<String> getExcludedMetricsPatterns() {
		return getStrings("jawap.metrics.excluded.pattern", "");
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
		return getPatternMap("jawap.groupUrls",
				"/\\d+:     /{id}," +
						"(.*).js:   *.js," +
						"(.*).css:  *.css," +
						"(.*).jpg:  *.jpg," +
						"(.*).jpeg: *.jpeg," +
						"(.*).png:  *.png");
	}

	private void loadProperties() {
		Properties defaultProperties = getProperties("jawap.properties");
		if (defaultProperties == null) {
			logger.error("Could not find jawap.properties in classpath");
			defaultProperties = new Properties();
		}
		// override values in default properties file
		final String jawapPropertyOverridesLocation = System.getProperty("jawap.property.overrides");
		if (jawapPropertyOverridesLocation != null) {
			logger.info("try loading of default property overrides: '" + jawapPropertyOverridesLocation + "'");
			properties = getProperties(jawapPropertyOverridesLocation, defaultProperties);
			if (properties == null) {
				logger.error("Could not find " + jawapPropertyOverridesLocation + " in classpath");
			}
		} else {
			properties = defaultProperties;
		}
	}

	private Properties getProperties(String classpathLocation) {
		return getProperties(classpathLocation, null);
	}

	private Properties getProperties(String classpathLocation, Properties defaultProperties) {
		if (classpathLocation == null) {
			return null;
		}
		final Properties props;
		if (defaultProperties != null) {
			props = new Properties(defaultProperties);
		} else {
			props = new Properties();
		}
		InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(classpathLocation);
		if (resourceStream != null) {
			try {
				props.load(resourceStream);
				return props;
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			} finally {
				try {
					resourceStream.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
		return null;
	}

	public String getString(final String key) {
		return getString(key, null);
	}

	public String getString(final String key, final String defaultValue) {
		return getAndCache(key, null, new PropertyLoader<String>() {
			@Override
			public String load() {
				return properties.getProperty(key, defaultValue);
			}
		});
	}

	public List<String> getLowerStrings(final String key, final String defaultValue) {
		return getAndCache(key, null, new PropertyLoader<List<String>>() {
			@Override
			public List<String> load() {
				String property = properties.getProperty(key, defaultValue);
				if (property != null && property.length() > 0) {
					final String[] split = property.split(",");
					for (int i = 0; i < split.length; i++) {
						split[i] = split[i].trim().toLowerCase();
					}
					return Arrays.asList(split);
				}
				return emptyList();
			}
		});
	}

	public List<Pattern> getPatterns(final String key, final String defaultValue) {
		final List<String> strings = getStrings(key, defaultValue);
		List<Pattern> patterns = new ArrayList<Pattern>(strings.size());
		for (String patternString : strings) {
			try {
				patterns.add(Pattern.compile(patternString));
			} catch (PatternSyntaxException e) {
				logger.error("Error while compiling pattern " + patternString, e);
			}
		}
		return patterns;
	}

	public List<String> getStrings(final String key, final String defaultValue) {
		return getAndCache(key, null, new PropertyLoader<List<String>>() {
			@Override
			public List<String> load() {
				String property = properties.getProperty(key, defaultValue);
				if (property != null && property.length() > 0) {
					final String[] split = property.split(",");
					for (int i = 0; i < split.length; i++) {
						split[i] = split[i].trim();
					}
					return Arrays.asList(split);
				}
				return emptyList();
			}
		});
	}

	public boolean getBoolean(final String key, final boolean defaultValue) {
		return getAndCache(key, defaultValue, new PropertyLoader<Boolean>() {
			@Override
			public Boolean load() {
				return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(defaultValue)));
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
				String value = properties.getProperty(key, Long.toString(defaultValue));
				try {
					return Long.parseLong(value);
				} catch (NumberFormatException e) {
					logger.error(e.getMessage(), e);
					return defaultValue;
				}
			}
		});
	}

	public Map<Pattern, String> getPatternMap(final String key, final String defaultValue) {
		return getAndCache(key, null, new PropertyLoader<Map<Pattern, String>>() {
			@Override
			public Map<Pattern, String> load() {
				String patternString = properties.getProperty(key, defaultValue);
				try {
					String[] groups = patternString.split(",");
					Map<Pattern, String> pattenGroupMap = new HashMap<Pattern, String>(groups.length);

					for (String group : groups) {
						group = group.trim();
						String[] keyValue = group.split(":");
						pattenGroupMap.put(Pattern.compile(keyValue[0].trim()), keyValue[1].trim());
					}
					return pattenGroupMap;
				} catch (RuntimeException e) {
					logger.error("Error while parsing pattern map. Expected format <regex>: <name>[, <regex>: <name>]. " +
							"Actual value: " + patternString, e);
					return Collections.emptyMap();
				}
			}
		});
	}

	private <T> T getAndCache(String key, T defaultValue, PropertyLoader<T> propertyLoader) {
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
