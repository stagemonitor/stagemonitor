package org.stagemonitor.jdbc;

import com.p6spy.engine.spy.P6Core;
import com.p6spy.engine.spy.P6SpyLoadableOptions;
import com.p6spy.engine.spy.P6SpyOptions;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.jdbc.p6spy.P6SpyMultiLogger;
import org.stagemonitor.jdbc.p6spy.StagemonitorP6Logger;

import javax.sql.DataSource;
import java.sql.Connection;

@Aspect
public class ConnectionMonitorAspect {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final boolean wrapConnections;

	public ConnectionMonitorAspect() {
		P6SpyMultiLogger.addLogger(new StagemonitorP6Logger());
		P6SpyLoadableOptions options = P6SpyOptions.getActiveInstance();
		P6SpyMultiLogger.addLogger(options.getAppenderInstance());
		options.setAppender(P6SpyMultiLogger.class.getCanonicalName());
		wrapConnections = options.getDriverNames() == null || options.getDriverNames().isEmpty();
		if (!wrapConnections) {
			logger.info("Stagemonitor will not wrap connections with p6spy wrappers, because p6spy is already " +
					"configured in your application");
		}
		P6Core.initialize();
	}

	@Pointcut(value = "call(java.sql.Connection javax.sql.DataSource.getConnection(..)) && !within(javax.sql.DataSource+)")
	public void dataSourceConnection() {
	}

	@Around(value = "dataSourceConnection()")
	public Object aroundGetConnection(ProceedingJoinPoint pjp) throws Throwable {
		Connection connection = null;
		final long start = System.nanoTime();
		try {
			connection = (Connection) pjp.proceed();
			return wrapConnections ? P6Core.wrapConnection(connection) : connection;
		} finally {
			if (connection != null) {
				DataSource dataSource = (DataSource) pjp.getTarget();
				ConnectionMonitor.monitorGetConnection(connection, dataSource, start);
			}
		}
	}
}
