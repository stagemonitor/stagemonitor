package org.stagemonitor.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

	private static final String STAGEMONITOR_ACTIVE = "stagemonitor.active";
	public static final String INTERNAL_MONITORING = "stagemonitor.internal.monitoring";
	private static final String REPORTING_INTERVAL_CONSOLE = "stagemonitor.reporting.interval.console";
	private static final String REPORTING_JMX = "stagemonitor.reporting.jmx";
	private static final String REPORTING_INTERVAL_GRAPHITE = "stagemonitor.reporting.interval.graphite";
	private static final String REPORTING_GRAPHITE_HOST_NAME = "stagemonitor.reporting.graphite.hostName";
	private static final String REPORTING_GRAPHITE_PORT = "stagemonitor.reporting.graphite.port";
	private static final String APPLICATION_NAME = "stagemonitor.applicationName";
	private static final String INSTANCE_NAME = "stagemonitor.instanceName";
	private static final String ELASTICSEARCH_URL = "stagemonitor.elasticsearch.url";
	private static final String METRICS_EXCLUDED_PATTERN = "stagemonitor.metrics.excluded.pattern";
	private static final String DISABLED_PLUGINS = "stagemonitor.plugins.disabled";

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private ConcurrentMap<String, Object> propertiesCache = new ConcurrentHashMap<String, Object>();
	private List<ConfigurationSource> configurationSources = new LinkedList<ConfigurationSource>();
	private Map<String, ConfigurationOption> configurationOptions = new LinkedHashMap<String, ConfigurationOption>();

	public Configuration() {
		configurationSources.add(new SystemPropertyConfigurationSource());
		configurationSources.add(new PropertyFileConfigurationSource());
		addCoreConfigurationOptions();
	}

	private void addCoreConfigurationOptions() {
		add(ConfigurationOption.builder()
				.key(STAGEMONITOR_ACTIVE)
				.dynamic(true)
				.label("Activate stagemonitor")
				.description("If set to 'false' stagemonitor will be completely deactivated.")
				.defaultValue("true")
				.build());
		add(ConfigurationOption.builder()
				.key(INTERNAL_MONITORING)
				.dynamic(true)
				.label("Internal monitoring")
				.description("If active, stagemonitor will collect internal performance data")
				.defaultValue("false")
				.build());
		add(ConfigurationOption.builder()
				.key(REPORTING_INTERVAL_CONSOLE)
				.dynamic(false)
				.label("Reporting interval console")
				.description("The amount of time between console reports (in seconds). " +
						"To deactivate console reports, set this to a value below 1.")
				.defaultValue("60")
				.build());
		add(ConfigurationOption.builder()
				.key(REPORTING_JMX)
				.dynamic(false)
				.label("Expose MBeans")
				.description("Whether or not to expose all metrics as MBeans.")
				.defaultValue("true")
				.build());
		add(ConfigurationOption.builder()
				.key(REPORTING_INTERVAL_GRAPHITE)
				.dynamic(false)
				.label("Reporting interval graphite")
				.description("The amount of time between the metrics are reported to graphite (in seconds).\n" +
						"To deactivate graphite reporting, set this to a value below 1, or don't provide " +
						"stagemonitor.reporting.graphite.hostName.")
				.defaultValue("60")
				.build());
		add(ConfigurationOption.builder()
				.key(REPORTING_GRAPHITE_HOST_NAME)
				.dynamic(false)
				.label("Graphite host name")
				.description("The name of the host where graphite is running. This setting is mandatory, if you want " +
						"to use the grafana dashboards.")
				.defaultValue(null)
				.build());
		add(ConfigurationOption.builder()
				.key(REPORTING_GRAPHITE_PORT)
				.dynamic(false)
				.label("Carbon port")
				.description("The port where carbon is listening.")
				.defaultValue("2003")
				.build());
		add(ConfigurationOption.builder()
				.key(APPLICATION_NAME)
				.dynamic(false)
				.label("Application name")
				.description("The name of the application.\n" +
						"Either this property or the display-name in web.xml is mandatory!")
				.defaultValue(null)
				.build());
		add(ConfigurationOption.builder()
				.key(INSTANCE_NAME)
				.dynamic(false)
				.label("Instance name")
				.description("The instance name.\n" +
						"If this property is not set, the instance name set to the first request's " +
						"javax.servlet.ServletRequest#getServerName()\n" +
						"That means that the collection of metrics does not start before the first request is executed!")
				.defaultValue(null)
				.build());
		add(ConfigurationOption.builder()
				.key(ELASTICSEARCH_URL)
				.dynamic(true)
				.label("Elasticsearch URL")
				.description("The URL of the elasticsearch server that stores the call stacks. If the URL is not " +
						"provided, the call stacks won't get stored.")
				.defaultValue(null)
				.build());
		add(ConfigurationOption.builder()
				.key(METRICS_EXCLUDED_PATTERN)
				.dynamic(true)
				.label("Excluded metrics (regex)")
				.description("A comma separated list of metric names that should not be collected.")
				.defaultValue("")
				.build());
		add(ConfigurationOption.builder()
				.key(DISABLED_PLUGINS)
				.dynamic(false)
				.label("Disabled plugins")
				.description("A comma separated list of plugin names (the simple class name) that should not be active.")
				.defaultValue("")
				.build());
	}

	public void add(ConfigurationOption configurationOption) {
		configurationOptions.put(configurationOption.getKey(), configurationOption);
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
		return getBoolean(STAGEMONITOR_ACTIVE);
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
		return getLong(REPORTING_INTERVAL_CONSOLE);
	}

	/**
	 * Whether or not to expose all metrics as MBeans.
	 *
	 * @return <code>true</code>, if all metrics should be exposed as MBeans, <code>false</code> otherwise
	 */
	public boolean reportToJMX() {
		return getBoolean(REPORTING_JMX);
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
		return getLong(REPORTING_INTERVAL_GRAPHITE);
	}

	/**
	 * The name of the host where graphite is running
	 * <p><b>This setting is mandatory, if you want to use the dashboard UI.</b></p>
	 *
	 * @return graphite's host's name
	 */
	public String getGraphiteHostName() {
		return getString(REPORTING_GRAPHITE_HOST_NAME);
	}

	/**
	 * The port where carbon is listening.
	 *
	 * @return the graphite carbon port
	 */
	public int getGraphitePort() {
		return getInt(REPORTING_GRAPHITE_PORT);
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
		return getString(APPLICATION_NAME);
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
		return getString(INSTANCE_NAME);
	}

	/**
	 * The URL of the elasticsearch server that stores the call stacks.
	 * If the URL is not provided, the call stacks won't get stored.
	 *
	 * @return the server url
	 */
	public String getElasticsearchUrl() {
		final String url = getString(ELASTICSEARCH_URL);
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
		return getPatterns(METRICS_EXCLUDED_PATTERN);
	}

	/**
	 * A comma separated list of plugin names (the simple class name) that should not be active.
	 *
	 * @return the disabled plugin names
	 */
	public Collection<String> getDisabledPlugins() {
		return getStrings(DISABLED_PLUGINS);
	}

	public String getString(final String key) {
		return getAndCache(key, new PropertyLoader<String>() {
			@Override
			public String load() {
				return getTrimmedProperty(key);
			}
		});
	}

	private String getTrimmedProperty(String key) {
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
			final ConfigurationOption configurationOption = configurationOptions.get(key);
			if (configurationOption == null) {
				logger.error("Configuration option with key '{}' ist not registered!", key);
				return null;
			}
			return configurationOption.getDefaultValue();
		}
	}

	public Collection<String> getLowerStrings(final String key) {
		return getAndCache(key, new PropertyLoader<Collection<String>>() {
			@Override
			public Collection<String> load() {
				String property = getTrimmedProperty(key);
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

	public Collection<Pattern> getPatterns(final String key) {
		final Collection<String> strings = getStrings(key);
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

	public Collection<String> getStrings(final String key) {
		return getAndCache(key, new PropertyLoader<Collection<String>>() {
			@Override
			public Collection<String> load() {
				String property = getTrimmedProperty(key);
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

	public boolean getBoolean(final String key) {
		return getAndCache(key, new PropertyLoader<Boolean>() {
			@Override
			public Boolean load() {
				return Boolean.parseBoolean(getTrimmedProperty(key));
			}
		});
	}

	public int getInt(String key) {
		return (int) getLong(key);
	}

	public long getLong(final String key) {
		return getAndCache(key, new PropertyLoader<Long>() {
			@Override
			public Long load() {
				String value = getTrimmedProperty(key);
				try {
					return Long.parseLong(value);
				} catch (NumberFormatException e) {
					logger.warn(e.getMessage() + " (this exception is ignored)", e);
					try {
						return Long.parseLong(configurationOptions.get(key).getDefaultValue());
					} catch (RuntimeException e2) {
						logger.warn(e2.getMessage() + " (this exception is ignored)", e);
						return -1L;
					}
				}
			}
		});
	}

	public Map<Pattern, String> getPatternMap(final String key) {
		return getAndCache(key, new PropertyLoader<Map<Pattern, String>>() {
			@Override
			public Map<Pattern, String> load() {
				String patternString = getTrimmedProperty(key);
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

	private <T> T getAndCache(String key, PropertyLoader<T> propertyLoader) {
		@SuppressWarnings("unchecked")
		T result = (T) propertiesCache.get(key);
		if (result == null) {
			result = propertyLoader.load();
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
