package org.stagemonitor.collector.jdbc;

import org.stagemonitor.collector.core.StageMonitorApplicationContext;
import org.stagemonitor.collector.profiler.Profiler;
import org.stagemonitor.util.GraphiteEncoder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Aspect
public class ConnectionMonitorAspect {

	public static final String METRIC_PREFIX = "jdbc.getconnection.";
	public static final String TIME = ".time";
	public static final String COUNT = ".count";
	public ConcurrentMap<DataSource, String> dataSourceUrlMap = new ConcurrentHashMap<DataSource, String>();
	                                                                                      // no proxies
	@Pointcut(value = "call(java.sql.Connection javax.sql.DataSource.getConnection(..)) && !within(javax.sql.DataSource+)")
	public void dataSourceConnection() {
	}

	@Around(value = "dataSourceConnection()")
	public Object aroundGetConnection(ProceedingJoinPoint pjp) throws Throwable {
		Connection connection = null;
		final long start = System.nanoTime();
		try {
			Profiler.start();
			connection = (Connection) pjp.proceed();
			return connection;
		} finally {
			Profiler.stop(pjp.getSignature().toString());
			long duration = System.nanoTime() - start;
			if (connection != null) {
				DataSource dataSource = ensureUrlExistsForDataSource(pjp, connection);
				String url = dataSourceUrlMap.get(dataSource);
				StageMonitorApplicationContext.getMetricRegistry().counter(METRIC_PREFIX + url + COUNT).inc();
				final long durationMs = TimeUnit.NANOSECONDS.toMillis(duration);
				StageMonitorApplicationContext.getMetricRegistry().counter(METRIC_PREFIX + url + TIME).inc(durationMs);
			}
		}
	}

	private DataSource ensureUrlExistsForDataSource(ProceedingJoinPoint pjp, Connection connection) throws SQLException {
		DataSource dataSource = (DataSource) pjp.getTarget();
		if (!dataSourceUrlMap.containsKey(dataSource)) {
			final DatabaseMetaData metaData = connection.getMetaData();
			dataSourceUrlMap.put(dataSource, GraphiteEncoder.encodeForGraphite(metaData.getURL() + "-" + metaData.getUserName()));
		}
		return dataSource;
	}
}
