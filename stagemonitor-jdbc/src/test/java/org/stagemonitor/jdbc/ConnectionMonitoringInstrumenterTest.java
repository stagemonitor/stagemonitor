package org.stagemonitor.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.SortedMap;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.Timer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.requestmonitor.MonitoredMethodRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;

public class ConnectionMonitoringInstrumenterTest {

	private static Connection connection;
	private TestDataSource dataSource;
	private CorePlugin corePlugin = mock(CorePlugin.class);
	private RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
	private RequestMonitor requestMonitor;

	@BeforeClass
	public static void attachProfiler() {
		Stagemonitor.init();
	}

	@Before
	public void setUp() throws Exception {
		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		when(requestMonitorPlugin.isCollectRequestStats()).thenReturn(true);
		when(requestMonitorPlugin.getCallStackEveryXRequestsToGroup()).thenReturn(1);
		when(requestMonitorPlugin.isProfilerActive()).thenReturn(true);

		Stagemonitor.getMetricRegistry().removeMatching(MetricFilter.ALL);

		dataSource = new TestDataSource();
		connection = mock(Connection.class);
		final DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		when(metaData.getURL()).thenReturn("jdbc:test");
		when(metaData.getUserName()).thenReturn("testUser");
		when(connection.getMetaData()).thenReturn(metaData);

		when(connection.prepareStatement(anyString())).thenReturn(mock(PreparedStatement.class));
		when(connection.createStatement()).thenReturn(mock(Statement.class));
		requestMonitor = new RequestMonitor(corePlugin, Stagemonitor.getMetricRegistry(), requestMonitorPlugin);
	}

	@AfterClass
	public static void cleanUp() {
		Stagemonitor.reset();
	}

	@Test
	public void monitorGetConnection() throws Exception {
		dataSource.getConnection();
		final SortedMap<String,Timer> timers = Stagemonitor.getMetricRegistry().getTimers();
		assertNotNull(timers.keySet().toString(), timers.get("getConnection.jdbc:test-testUser"));
	}

	@Test
	public void monitorGetConnectionUsernamePassword() throws Exception {
		dataSource.getConnection("user", "pw");
		final SortedMap<String,Timer> timers = Stagemonitor.getMetricRegistry().getTimers();
		assertNotNull(timers.keySet().toString(), timers.get("getConnection.jdbc:test-testUser"));
	}

	@Test
	public void testRecordSqlPreparedStatement() throws Exception {
		final RequestMonitor.RequestInformation<RequestTrace> requestInformation = requestMonitor
				.monitor(new MonitoredMethodRequest("testRecordSql()", new MonitoredMethodRequest.MethodExecution() {
					@Override
					public Object execute() throws Exception {
						Profiler.start("public void org.stagemonitor.jdbc.ConnectionMonitoringInstrumenterTest.testRecordSql()");
						dataSource.getConnection().prepareStatement("SELECT * from STAGEMONITOR").execute();
						Profiler.stop();
						return null;
					}
				}));
		final SortedMap<String,Timer> timers = Stagemonitor.getMetricRegistry().getTimers();
		assertTrue(timers.keySet().toString(), timers.size() > 1);
		assertNotNull(timers.keySet().toString(), timers.get("db.All.time.statement"));
		assertNotNull(timers.keySet().toString(), timers.get("db.ConnectionMonitoringInstrumenterTest#testRecordSql.time.statement"));
		final CallStackElement callStack = requestInformation.getRequestTrace().getCallStack();
		assertEquals("testRecordSql()", callStack.getSignature());
		assertEquals("public void org.stagemonitor.jdbc.ConnectionMonitoringInstrumenterTest.testRecordSql()",
				callStack.getChildren().get(0).getSignature());
		assertEquals("SELECT * from STAGEMONITOR ", callStack.getChildren().get(0).getChildren().get(0).getSignature());
	}

	@Test
	public void testRecordSqlStatement() throws Exception {
		final RequestMonitor.RequestInformation<RequestTrace> requestInformation = requestMonitor
				.monitor(new MonitoredMethodRequest("testRecordSql()", new MonitoredMethodRequest.MethodExecution() {
					@Override
					public Object execute() throws Exception {
						Profiler.start("public void org.stagemonitor.jdbc.ConnectionMonitoringInstrumenterTest.testRecordSql()");
						dataSource.getConnection().createStatement().execute("SELECT * from STAGEMONITOR");
						Profiler.stop();
						return null;
					}
				}));
		final SortedMap<String,Timer> timers = Stagemonitor.getMetricRegistry().getTimers();
		assertTrue(timers.keySet().toString(), timers.size() > 1);
		assertNotNull(timers.keySet().toString(), timers.get("db.All.time.statement"));
		assertNotNull(timers.keySet().toString(), timers.get("db.ConnectionMonitoringInstrumenterTest#testRecordSql.time.statement"));
		final CallStackElement callStack = requestInformation.getRequestTrace().getCallStack();
		assertEquals("testRecordSql()", callStack.getSignature());
		assertEquals("public void org.stagemonitor.jdbc.ConnectionMonitoringInstrumenterTest.testRecordSql()",
				callStack.getChildren().get(0).getSignature());
		assertEquals("SELECT * from STAGEMONITOR ", callStack.getChildren().get(0).getChildren().get(0).getSignature());
	}

	private static class AbstractTestDataSource {

		public Connection getConnection() throws SQLException {
			return doGetConnection();
		}

		// private method to ensure that the instrumenter does not copy the getConnection method
		// but rather calls the method from the super class because a direct call to doGetConnection would fail
		private Connection doGetConnection() throws SQLException {
			return connection;
		}
	}

	private static class TestDataSource extends AbstractTestDataSource implements DataSource {

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			return connection;
		}

		@Override
		public PrintWriter getLogWriter() throws SQLException {
			return null;
		}

		@Override
		public void setLogWriter(PrintWriter out) throws SQLException {
		}

		@Override
		public void setLoginTimeout(int seconds) throws SQLException {
		}

		@Override
		public int getLoginTimeout() throws SQLException {
			return 0;
		}

		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			return null;
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return null;
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return false;
		}
	}
}
