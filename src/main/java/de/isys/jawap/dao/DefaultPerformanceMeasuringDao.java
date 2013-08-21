package de.isys.jawap.dao;

import de.isys.jawap.model.HttpRequestStats;
import de.isys.jawap.model.MethodCallStats;
import de.isys.jawap.model.PerformanceMeasurementSession;
import de.isys.jawap.model.PeriodicPerformanceData;
import de.isys.jawap.model.ThreadPoolMetrics;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DefaultPerformanceMeasuringDao implements PerformanceMeasuringDao {

	private final DataSource dataSource;
	private static final String INSERT_PERF_MEASUREMENT =
			" INSERT INTO PERFORMANCE_MEASUREMENT (" +
					" PERFORMANCE_MEASUREMENT_ID " +
					",START_OF_SESSION" +
					",END_OF_SESSION" +
					",CPU_USAGE_PERCENT" +
					",GARBAGE_COLLECTIONS_COUNT" +
					",GARBAGE_COLLECTION_TIME" +
					") VALUES (?,?,?,?,?,?)";

	public DefaultPerformanceMeasuringDao(DataSource dataSource) {
		this.dataSource = dataSource;
	}


	@Override
	public void save(PerformanceMeasurementSession performanceMeasurementSession) {
		try {
			performanceMeasurementSession.setId(UUID.randomUUID().toString());
			final PreparedStatement preparedStatement = dataSource.getConnection().prepareStatement(INSERT_PERF_MEASUREMENT);
			preparedStatement.setString(1, performanceMeasurementSession.getId());
			preparedStatement.setTimestamp(2, new Timestamp(performanceMeasurementSession.getStartOfSession().getTime()));
			preparedStatement.setTimestamp(3, performanceMeasurementSession.getEndOfSession() != null ? new Timestamp
					(performanceMeasurementSession.getEndOfSession().getTime()) : null);
			preparedStatement.setFloat(4, performanceMeasurementSession.getCpuUsagePercent());
			preparedStatement.setLong(5, performanceMeasurementSession.getGarbageCollectionsCount());
			preparedStatement.setLong(6, performanceMeasurementSession.getGarbageCollectionTime());
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void save(HttpRequestStats requestStats) {
		requestStats.setId(UUID.randomUUID().toString());

		update(" INSERT INTO REQUEST_STATS (" +
				" REQUEST_STATS_ID " +
				",PERFORMANCE_MEASUREMENT_ID" +
				",URL" +
				",QUERY_PARAMS" +
				",TIMESTAMP" +
				",DURATION" +
				",STATUS) VALUES (?,?,?,?,?,?,?)",
				requestStats.getId(),
				requestStats.getPerformanceMeasurementSession().getId(),
				requestStats.getUrl(),
				requestStats.getQueryParams(),
				new Timestamp(requestStats.getTimestamp()),
				Long.valueOf(requestStats.getExecutionTime()),
				requestStats.getStatusCode()
		);

		for (MethodCallStats methodCallStats : requestStats.getMethodCallStats()) {
			save(methodCallStats);
		}
	}

	private void update(String sql, Object... args) {
		// TODO
	}

	private void batchUpdate(String sql, Object... args) {
		// TODO
	}


	public void save(MethodCallStats methodCallStats) {
		List<Object[]> batchArgs = new ArrayList<Object[]>();
		fillBatchArgsRek(batchArgs, methodCallStats);

		batchUpdate(" INSERT INTO METHOD_CALL_STATS (" +
				" METHOD_CALL_STATS_ID " +
				",REQUEST_STATS_ID" +
				",PARENT_ID" +
				",SIGNATURE" +
				",CLASS_NAME" +
				",METHOD_NAME" +
				",EXECUTION_TIME" +
				",NET_EXECUTION_TIME" +
				",TIMESTAMP" +
				") VALUES (?,?,?,?,?,?,?,?,?)",
				batchArgs
		);
	}

	private void fillBatchArgsRek(List<Object[]> batchArgsList, MethodCallStats methodCallStats) {
		methodCallStats.setId(UUID.randomUUID().toString());
		Object[] batchArgs = new Object[]{
				methodCallStats.getId(),
				methodCallStats.getRequestStats().getId(),
				methodCallStats.parent.getId(),
				methodCallStats.getClassName(),
				methodCallStats.getMethodName(),
				Long.valueOf(methodCallStats.getExecutionTime()),
				Long.valueOf(methodCallStats.getNetExecutionTime()),
				new Timestamp(methodCallStats.getTimestamp())
		};
		batchArgsList.add(batchArgs);

		for (MethodCallStats callStats : methodCallStats.getChildren()) {
			fillBatchArgsRek(batchArgsList, callStats);
		}
	}

	@Override
	public void update(PerformanceMeasurementSession performanceMeasurementSession) {
		update("UPDATE PERFORMANCE_MEASUREMENT SET " +
				"START_OF_SESSION = ? " +
				",END_OF_SESSION = ? " +
				",CPU_USAGE_PERCENT = ? " +
				",GARBAGE_COLLECTIONS_COUNT = ? " +
				",GARBAGE_COLLECTION_TIME = ? " +
				" WHERE PERFORMANCE_MEASUREMENT_ID = ? ",
				new Timestamp(performanceMeasurementSession.getStartOfSession().getTime()),
				new Timestamp(performanceMeasurementSession.getEndOfSession().getTime()),
				performanceMeasurementSession.getCpuUsagePercent(),
				performanceMeasurementSession.getGarbageCollectionsCount(),
				performanceMeasurementSession.getGarbageCollectionTime(),
				performanceMeasurementSession.getId()
		);
	}

	@Override
	public void save(PeriodicPerformanceData periodicPerformanceData) {
		periodicPerformanceData.setId(UUID.randomUUID().toString());

		ThreadPoolMetrics appServerThreadPoolMetrics = periodicPerformanceData.getAppServerThreadPoolMetrics();
		ThreadPoolMetrics springThreadPoolMetrics = periodicPerformanceData.getSpringThreadPoolMetrics();
		ThreadPoolMetrics springScheduledThreadPoolMetrics = periodicPerformanceData
				.getSpringScheduledThreadPoolMetrics();

		save(appServerThreadPoolMetrics);
		save(springThreadPoolMetrics);
		save(springScheduledThreadPoolMetrics);

		update(" INSERT INTO PERIODIC_PERFORMANCE_DATA (" +
				" PERIODIC_PERFORMANCE_DATA_ID " +
				",PERFORMANCE_MEASUREMENT_ID" +
				",TOTAL_MEMORY" +
				",FREE_MEMORY" +
				",CPU_USAGE_PERCENT" +
				",APP_SERVER_POOL_METRICS_ID" +
				",SHOP_JDBC_POOL_METRICS_ID" +
				",WEBSTAGE_JDBC_POOL_METRICS_ID" +
				",SPRING_POOL_METRICS_ID" +
				",SPRING_SCHED_POOL_METRICS_ID" +
				",TIME" +
				") VALUES (?,?,?,?,?,?,?,?,?,?,?)",
				periodicPerformanceData.getId(),
				periodicPerformanceData.getPerformanceMeasurementSession().getId(),
				Long.valueOf(periodicPerformanceData.getTotalMemory()),
				Long.valueOf(periodicPerformanceData.getFreeMemory()),
				Float.valueOf(periodicPerformanceData.getCpuUsagePercent()),
				appServerThreadPoolMetrics != null ? appServerThreadPoolMetrics.getId() : null,
				null,
				null,
				springThreadPoolMetrics != null ? springThreadPoolMetrics.getId() : null,
				springScheduledThreadPoolMetrics != null ? springScheduledThreadPoolMetrics.getId() : null,
				new Timestamp(System.currentTimeMillis())
		);
	}

	private void save(ThreadPoolMetrics threadPoolMetrics) {
		if (threadPoolMetrics == null) {
			return;
		}
		threadPoolMetrics.setId(UUID.randomUUID().toString());
		update("INSERT INTO THREAD_POOL_METRICS (" +
				"THREAD_POOL_METRICS_ID" +
				",THREAD_POOL_MAX_SIZE" +
				",THREAD_POOL_SIZE" +
				",THREAD_POOL_ACTIVE_THREADS" +
				",THREAD_POOL_TASKS_PENDING" +
				") VALUES (?,?,?,?,?)",
				threadPoolMetrics.getId(),
				Integer.valueOf(threadPoolMetrics.getMaxPoolSize()),
				Integer.valueOf(threadPoolMetrics.getThreadPoolSize()),
				Integer.valueOf(threadPoolMetrics.getThreadPoolNumActiveThreads()),
				threadPoolMetrics.getThreadPoolNumTasksPending()
		);
	}
}
