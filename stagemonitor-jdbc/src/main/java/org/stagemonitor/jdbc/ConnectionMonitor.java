package org.stagemonitor.jdbc;

import static com.codahale.metrics.MetricRegistry.name;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;

import com.codahale.metrics.MetricRegistry;
import com.p6spy.engine.spy.P6Core;
import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.option.EnvironmentVariables;
import com.p6spy.engine.spy.option.SpyDotProperties;
import com.p6spy.engine.spy.option.SystemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.GraphiteSanitizer;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.jdbc.p6spy.P6SpyMultiLogger;
import org.stagemonitor.jdbc.p6spy.StagemonitorP6Logger;

public class ConnectionMonitor {


	private final Logger logger = LoggerFactory.getLogger(ConnectionMonitor.class);
	private final boolean collectSql;

	private ConcurrentMap<DataSource, String> dataSourceUrlMap = new ConcurrentHashMap<DataSource, String>();

	private MetricRegistry metricRegistry;

	private final boolean active;

	public ConnectionMonitor(Configuration configuration, MetricRegistry metricRegistry) {
		this.metricRegistry = metricRegistry;
		collectSql = configuration.getConfig(JdbcPlugin.class).isCollectSql();
		final Map<String, String> p6SpyOptions = getP6SpyOptions();
		final boolean p6SpyAlreadyConfigured = !StringUtils.isEmpty(p6SpyOptions.get(P6SpyOptions.DRIVER_NAMES));
		if (p6SpyAlreadyConfigured) {
			logger.warn("It seem like you already have p6spy configured. Using p6spy and stagemonitor is not supported. " +
					"You won't be able to see SQL queries in the call tree.");
		}
		if (!p6SpyAlreadyConfigured && ConnectionMonitor.isActive(configuration.getConfig(CorePlugin.class))) {
			active = true;
			if (collectSql) {
				unregisterP6SpyMBeans();
				System.setProperty(SystemProperties.P6SPY_PREFIX + P6SpyOptions.APPENDER, P6SpyMultiLogger.class.getName());
				P6SpyMultiLogger.addLogger(new StagemonitorP6Logger(configuration, this.metricRegistry));
				P6Core.initialize();
			}
		} else {
			active = false;
		}
	}

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

	private void unregisterP6SpyMBeans() {
		final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			for (ObjectName objectName : mbs.queryNames(new ObjectName("com.p6spy.*:name=*"), null)) {
				mbs.unregisterMBean(objectName);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Connection monitorGetConnection(Connection connection, DataSource dataSource, long duration) throws SQLException {
		if (!active) {
			return connection;
		}
		ensureUrlExistsForDataSource(dataSource, connection);
		String url = dataSourceUrlMap.get(dataSource);
		metricRegistry.timer(name("getConnection", url)).update(duration, TimeUnit.NANOSECONDS);
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
