package org.stagemonitor.jdbc;

import com.codahale.metrics.MetricRegistry;
import com.p6spy.engine.spy.P6Core;
import com.p6spy.engine.spy.P6SpyLoadableOptions;
import com.p6spy.engine.spy.P6SpyOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.StageMonitor;
import org.stagemonitor.core.util.GraphiteSanitizer;
import org.stagemonitor.jdbc.p6spy.P6SpyMultiLogger;
import org.stagemonitor.jdbc.p6spy.StagemonitorP6Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

public class ConnectionMonitor {

	private static final boolean ACTIVE = ConnectionMonitor.isActive(StageMonitor.getConfiguration());

	private final Logger logger = LoggerFactory.getLogger(ConnectionMonitor.class);

	private ConcurrentMap<DataSource, String> dataSourceUrlMap = new ConcurrentHashMap<DataSource, String>();

	private MetricRegistry metricRegistry;

	private final boolean p6SpyAlreadyConfigured;

	public ConnectionMonitor(Configuration configuration, MetricRegistry metricRegistry) {
		this.metricRegistry = metricRegistry;

		if (ACTIVE && StageMonitor.getConfiguration().getBoolean(JdbcPlugin.COLLECT_SQL)) {
			unregisterP6SpyMBeans();
			P6SpyLoadableOptions options = P6SpyOptions.getActiveInstance();
			addStagemonitorLogger(configuration, options);
			p6SpyAlreadyConfigured = options.getDriverNames() == null || options.getDriverNames().isEmpty();
			if (!p6SpyAlreadyConfigured) {
				logger.info("Stagemonitor will not wrap connections with p6spy wrappers, because p6spy is already " +
						"configured in your application");
			}
			P6Core.initialize();
		} else {
			p6SpyAlreadyConfigured = false;
		}
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

	private void addStagemonitorLogger(Configuration configuration, P6SpyLoadableOptions options) {
		P6SpyMultiLogger.addLogger(new StagemonitorP6Logger(configuration, metricRegistry));
		if (p6SpyAlreadyConfigured) {
			P6SpyMultiLogger.addLogger(options.getAppenderInstance());
		}
		options.setAppender(P6SpyMultiLogger.class.getCanonicalName());
	}

	public Connection monitorGetConnection(Connection connection, DataSource dataSource, long duration) throws SQLException {
		ensureUrlExistsForDataSource(dataSource, connection);
		String url = dataSourceUrlMap.get(dataSource);
		metricRegistry.timer(name("getConnection", url)).update(duration, TimeUnit.NANOSECONDS);
		return p6SpyAlreadyConfigured ? P6Core.wrapConnection(connection) : connection;
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

	public static boolean isActive(Configuration configuration) {
		return !configuration.getDisabledPlugins().contains(JdbcPlugin.class.getSimpleName()) &&
				configuration.isStagemonitorActive();
	}
}
