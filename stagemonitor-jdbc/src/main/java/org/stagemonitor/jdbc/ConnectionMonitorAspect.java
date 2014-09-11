package org.stagemonitor.jdbc;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StageMonitor;

import javax.sql.DataSource;
import java.sql.Connection;

@Aspect
public class ConnectionMonitorAspect {

	private static final boolean ACTIVE = ConnectionMonitor.isActive(StageMonitor.getConfiguration(CorePlugin.class));

	private ConnectionMonitor connectionMonitor;


	public ConnectionMonitorAspect() {
		connectionMonitor = new ConnectionMonitor(StageMonitor.getConfiguration(), StageMonitor.getMetricRegistry());
	}

	@Pointcut(value = "call(java.sql.Connection javax.sql.DataSource.getConnection(..)) && !within(javax.sql.DataSource+)")
	public void dataSourceConnection() {
	}

	@Pointcut("if()")
	public static boolean ifActivated() {
		return ACTIVE;
	}

	@Around(value = "ifActivated() && dataSourceConnection()")
	public Object aroundGetConnection(ProceedingJoinPoint pjp) throws Throwable {
		final long start = System.nanoTime();
		Connection connection = (Connection) pjp.proceed();
		return connectionMonitor.monitorGetConnection(connection, (DataSource) pjp.getTarget(), System.nanoTime() - start);
	}
}
