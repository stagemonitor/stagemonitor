package org.stagemonitor.jdbc;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.StageMonitor;
import org.stagemonitor.core.util.GraphiteSanitizer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

public class ConnectionMonitor {

	private final static Logger log = LoggerFactory.getLogger(ConnectionMonitor.class);

	private static ConcurrentMap<DataSource, String> dataSourceUrlMap = new ConcurrentHashMap<DataSource, String>();
	private static MetricRegistry metricRegistry = StageMonitor.getMetricRegistry();

	public static void monitorGetConnection(Connection connection, DataSource dataSource, long duration) {
		if (connection != null) {
			ensureUrlExistsForDataSource(dataSource, connection);
			String url = dataSourceUrlMap.get(dataSource);
			metricRegistry.timer(name("getConnection", url)).update(duration, TimeUnit.NANOSECONDS);
		}
	}

	private static DataSource ensureUrlExistsForDataSource(DataSource dataSource, Connection connection) {
		if (!dataSourceUrlMap.containsKey(dataSource)) {
			final DatabaseMetaData metaData;
			try {
				metaData = connection.getMetaData();
				dataSourceUrlMap.put(dataSource, GraphiteSanitizer.sanitizeGraphiteMetricSegment(metaData.getURL() + "-" + metaData.getUserName()));
			} catch (SQLException e) {
				log.warn(e.getMessage(), e);
			}
		}
		return dataSource;
	}
}
