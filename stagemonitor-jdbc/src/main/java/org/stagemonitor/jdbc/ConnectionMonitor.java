package org.stagemonitor.jdbc;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.p6spy.engine.spy.P6Core;
import com.p6spy.engine.spy.P6SpyLoadableOptions;
import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.option.EnvironmentVariables;
import com.p6spy.engine.spy.option.SpyDotProperties;
import com.p6spy.engine.spy.option.SystemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.GraphiteSanitizer;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.jdbc.p6spy.P6SpyMultiLogger;
import org.stagemonitor.jdbc.p6spy.StagemonitorP6Logger;

public class ConnectionMonitor {


	private final Logger logger = LoggerFactory.getLogger(ConnectionMonitor.class);
	private final boolean collectSql;

	private ConcurrentMap<DataSource, String> dataSourceUrlMap = new ConcurrentHashMap<DataSource, String>();

	private Metric2Registry metricRegistry;

	private final boolean active;

	public ConnectionMonitor(Configuration configuration, Metric2Registry metricRegistry) {
		this.metricRegistry = metricRegistry;
		collectSql = configuration.getConfig(JdbcPlugin.class).isCollectSql();
		final Map<String, String> p6SpyOptions = getP6SpyOptions();
		final boolean p6SpyAlreadyConfigured = !StringUtils.isEmpty(p6SpyOptions.get(P6SpyOptions.DRIVER_NAMES));
		if (p6SpyAlreadyConfigured) {
			logger.warn("It seems like you already have p6spy configured. Using p6spy and stagemonitor is not supported. " +
					"You won't be able to see SQL queries in the call tree.");
		}
		if (!p6SpyAlreadyConfigured && ConnectionMonitor.isActive(configuration.getConfig(CorePlugin.class))) {
			active = true;
			if (collectSql) {
				// set p6spy options before p6spy is initialized
				// this avoids that spy.log is being created
				System.setProperty(SystemProperties.P6SPY_PREFIX + P6SpyOptions.JMX, Boolean.FALSE.toString());
				System.setProperty(SystemProperties.P6SPY_PREFIX + P6SpyOptions.APPENDER, P6SpyMultiLogger.class.getName());
				P6SpyMultiLogger.addLogger(new StagemonitorP6Logger(configuration, this.metricRegistry));
				P6SpyLoadableOptions options = P6SpyOptions.getActiveInstance();
				// p6spy might already been initialized
				// for example, wildfily loads all drivers and thus the P6SpyDriver on startup
				// which happens before stagemonitor's initialisation
				options.setAppender(P6SpyMultiLogger.class.getName());
				P6Core.initialize();
			}
		} else {
			active = false;
		}
	}

	/**
	 * Gets the configuration options of p6spy without initializing it, because otherwise a spy.log file would be created.
	 *
	 * @return the configuration options of p6spy
	 */
	private Map<String, String> getP6SpyOptions() {
		Map<String, String> p6SpyOptions = new HashMap<String, String>();
		p6SpyOptions.putAll(P6SpyOptions.defaults);
		try {
			final Map<String, String> optionsFromSpyProperties = new SpyDotProperties().getOptions();
			if (optionsFromSpyProperties != null) {
				p6SpyOptions.putAll(optionsFromSpyProperties);
			}
		} catch (IOException e) {
			// ignore
		}
		p6SpyOptions.putAll(new EnvironmentVariables().getOptions());
		p6SpyOptions.putAll(new SystemProperties().getOptions());
		return p6SpyOptions;
	}

	public Connection monitorGetConnection(Connection connection, DataSource dataSource, long duration) throws SQLException {
		if (!active) {
			return connection;
		}
		ensureUrlExistsForDataSource(dataSource, connection);
		String url = dataSourceUrlMap.get(dataSource);
		metricRegistry.timer(name("get_jdbc_connection").tag("url", url).build()).update(duration, TimeUnit.NANOSECONDS);
		return collectSql ? P6Core.wrapConnection(connection) : connection;
	}

	private DataSource ensureUrlExistsForDataSource(DataSource dataSource, Connection connection) {
		if (!dataSourceUrlMap.containsKey(dataSource)) {
			final DatabaseMetaData metaData;
			try {
				metaData = connection.getMetaData();
				dataSourceUrlMap.put(dataSource, GraphiteSanitizer.sanitizeGraphiteMetricSegment(metaData.getURL() + "-" + metaData.getUserName()));
			} catch (SQLException e) {
				logger.warn(e.getMessage(), e);
			}
		}
		return dataSource;
	}

	public static boolean isActive(CorePlugin corePlugin) {
		return !corePlugin.getDisabledPlugins().contains(JdbcPlugin.class.getSimpleName()) &&
				corePlugin.isStagemonitorActive();
	}
}
