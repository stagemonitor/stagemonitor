package org.stagemonitor.jdbc;

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

public class ConnectionMonitor {

	private final static Logger log = LoggerFactory.getLogger(ConnectionMonitor.class);

	private static final String METRIC_PREFIX = "jdbc.getconnection.";
	private static final String TIME = ".time";
	private static final String COUNT = ".count";
	private static ConcurrentMap<DataSource, String> dataSourceUrlMap = new ConcurrentHashMap<DataSource, String>();

	public static void monitorGetConnection(Connection connection, DataSource dataSource, long startNanoTime) {
		long duration = System.nanoTime() - startNanoTime;
		if (connection != null) {
			ensureUrlExistsForDataSource(dataSource, connection);
			String url = dataSourceUrlMap.get(dataSource);
			StageMonitor.getMetricRegistry().counter(METRIC_PREFIX + url + COUNT).inc();
			final long durationMs = TimeUnit.NANOSECONDS.toMillis(duration);
			StageMonitor.getMetricRegistry().counter(METRIC_PREFIX + url + TIME).inc(durationMs);
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
