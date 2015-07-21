package org.stagemonitor.core.instrument;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import org.stagemonitor.core.Stagemonitor;

/**
 * This is not a real Driver, it is just used to attach the stagemonitor agent as early in the lifecycle as possible.
 * Because the earlier the agent is attached, the less classes have to be retransformed, which is a expensive operation.
 *
 * Some application server as wildfly load all Driver implementations with a ServiceLoader at startup and even
 * before ServletContainerInitializer classes are loaded.
 */
public class StagemonitorRuntimeAgentAttacherDriver implements Driver {

	static  {
		Stagemonitor.init();
	}

	@Override
	public Connection connect(String url, Properties info) throws SQLException {
		return null;
	}

	@Override
	public boolean acceptsURL(String url) throws SQLException {
		return false;
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		return new DriverPropertyInfo[0];
	}

	@Override
	public int getMajorVersion() {
		return 0;
	}

	@Override
	public int getMinorVersion() {
		return 0;
	}

	@Override
	public boolean jdbcCompliant() {
		return false;
	}

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException();
	}
}
