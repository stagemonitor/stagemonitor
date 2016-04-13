package org.stagemonitor.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import net.bytebuddy.asm.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultConnectionMonitoringTransformer extends ConnectionMonitoringTransformer {

	private static final Logger logger = LoggerFactory.getLogger(DefaultConnectionMonitoringTransformer.class);

	public DefaultConnectionMonitoringTransformer() throws NoSuchMethodException {
		super();
	}

	@Advice.OnMethodEnter
	private static long addTimestampLocalVariable() {
		return System.nanoTime();
	}

	@Advice.OnMethodExit
	private static void addDirectMonitorMethodCall(@Advice.This Object dataSource,
												   @Advice.Return(readOnly = false) Connection connection,
												   @Advice.Enter long startTime) {
		connection = monitorGetConnection(dataSource, connection, startTime);
	}

	public static Connection monitorGetConnection(Object dataSource, Connection connection, long startTime) {
		if (!(dataSource instanceof DataSource)) {
			return connection;
		}
		try {
			return ConnectionMonitoringTransformer.connectionMonitor
					.monitorGetConnection(connection, (DataSource) dataSource, System.nanoTime() - startTime);
		} catch (SQLException e) {
			logger.warn(e.getMessage(), e);
			return connection;
		}
	}
}
