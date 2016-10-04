package org.stagemonitor.jdbc;

import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.event.CompoundJdbcEventListener;
import com.p6spy.engine.event.DefaultEventListener;
import com.p6spy.engine.event.JdbcEventListener;
import com.p6spy.engine.wrapper.ConnectionWrapper;
import com.p6spy.engine.wrapper.P6Proxy;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import javax.sql.DataSource;

public class ConnectionMonitor {

	private final boolean active;
	private final JdbcEventListener jdbcEventListener;

	public ConnectionMonitor(Configuration configuration) {
		active = ConnectionMonitor.isActive(configuration.getConfig(CorePlugin.class));
		jdbcEventListener = new CompoundJdbcEventListener(Arrays.asList(DefaultEventListener.INSTANCE, new StagemonitorJdbcEventListener(configuration)));
	}

	public Connection monitorGetConnection(Connection connection, Object dataSource, long duration) throws SQLException {
		if (active && dataSource instanceof DataSource && !(connection instanceof P6Proxy)) {
			return ConnectionWrapper.wrap(connection, jdbcEventListener, ConnectionInformation.fromDataSource((DataSource) dataSource, connection, duration));
		} else {
			return connection;
		}
	}

	public static boolean isActive(CorePlugin corePlugin) {
		return !corePlugin.getDisabledPlugins().contains(JdbcPlugin.class.getSimpleName()) &&
				corePlugin.isStagemonitorActive();
	}
}
