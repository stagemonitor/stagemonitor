package de.isys.jawap.collector.jdbc;

import de.isys.jawap.collector.core.ApplicationContext;
import de.isys.jawap.collector.profiler.Profiler;
import de.isys.jawap.entities.profiler.CallStackElement;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
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
		try {
			Profiler.start();
			connection = (Connection) pjp.proceed();
			return connection;
		} finally {
			final CallStackElement callStackElement = Profiler.stop(pjp.getSignature().getDeclaringTypeName(), pjp.getSignature().toString());
			if (connection != null && callStackElement != null) {
				DataSource dataSource = ensureUrlExistsForDataSource(pjp, connection);
				String url = dataSourceUrlMap.get(dataSource);
				ApplicationContext.getMetricRegistry().counter(METRIC_PREFIX + url + COUNT).inc();
				final long durationMs = TimeUnit.NANOSECONDS.toMillis(callStackElement.getExecutionTime());
				ApplicationContext.getMetricRegistry().counter(METRIC_PREFIX + url + TIME).inc(durationMs);
			}
		}
	}

	private DataSource ensureUrlExistsForDataSource(ProceedingJoinPoint pjp, Connection connection) throws SQLException {
		DataSource dataSource = (DataSource) pjp.getTarget();
		if (!dataSourceUrlMap.containsKey(dataSource)) {
			final DatabaseMetaData metaData = connection.getMetaData();
			dataSourceUrlMap.put(dataSource, metaData.getURL() + "-" + metaData.getUserName());
		}
		return dataSource;
	}
}
