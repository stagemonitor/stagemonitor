package org.stagemonitor.jdbc;

import com.p6spy.engine.event.CompoundJdbcEventListener;
import com.p6spy.engine.event.DefaultEventListener;
import com.p6spy.engine.event.JdbcEventListener;
import com.p6spy.engine.wrapper.ConnectionWrapper;
import com.p6spy.engine.wrapper.P6Proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class ConnectionMonitor {


	private final Logger logger = LoggerFactory.getLogger(ConnectionMonitor.class);
	private final boolean collectSql;
	private final MetricName.MetricNameTemplate getConnectionTemplate = name("get_jdbc_connection").templateFor("url");

	private ConcurrentMap<DataSource, String> dataSourceUrlMap = new ConcurrentHashMap<DataSource, String>();

	private Metric2Registry metricRegistry;

	private final boolean active;

	private final JdbcEventListener jdbcEventListener;

	public ConnectionMonitor(Configuration configuration, Metric2Registry metricRegistry) {
		this.metricRegistry = metricRegistry;
		collectSql = configuration.getConfig(JdbcPlugin.class).isCollectSql();
		active = ConnectionMonitor.isActive(configuration.getConfig(CorePlugin.class));
		jdbcEventListener = new CompoundJdbcEventListener(Arrays.asList(DefaultEventListener.INSTANCE, new StagemonitorJdbcEventListener(configuration)));
	}

	public Connection monitorGetConnection(Connection connection, Object dataSource, long duration) throws SQLException {
		if (active && dataSource instanceof DataSource && !(connection instanceof P6Proxy)) {
			ensureUrlExistsForDataSource((DataSource) dataSource, connection);
			String url = dataSourceUrlMap.get(dataSource);
			metricRegistry.timer(getConnectionTemplate.build(url)).update(duration, TimeUnit.NANOSECONDS);
			return collectSql ? new ConnectionWrapper(connection, jdbcEventListener) : connection;
		} else {
			return connection;
		}
	}

	private DataSource ensureUrlExistsForDataSource(DataSource dataSource, Connection connection) {
		if (!dataSourceUrlMap.containsKey(dataSource)) {
			final DatabaseMetaData metaData;
			try {
				metaData = connection.getMetaData();
				dataSourceUrlMap.put(dataSource, metaData.getUserName() + '@' + metaData.getURL());
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
