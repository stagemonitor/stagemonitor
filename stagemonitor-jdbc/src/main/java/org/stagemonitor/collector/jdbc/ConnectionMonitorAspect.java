package org.stagemonitor.collector.jdbc;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import javax.sql.DataSource;
import java.sql.Connection;

@Aspect
public class ConnectionMonitorAspect {
	                                                                                      // no proxies
	@Pointcut(value = "call(java.sql.Connection javax.sql.DataSource.getConnection(..)) && !within(javax.sql.DataSource+)")
	public void dataSourceConnection() {
	}

	@Around(value = "dataSourceConnection()")
	public Object aroundGetConnection(ProceedingJoinPoint pjp) throws Throwable {
		Connection connection = null;
		final long start = System.nanoTime();
		try {
			connection = (Connection) pjp.proceed();
			return connection;
		} finally {
			if (connection != null) {
				DataSource dataSource = (DataSource) pjp.getTarget();
				ConnectionMonitor.monitorGetConnection(connection, dataSource, start);
			}
		}
	}
}
