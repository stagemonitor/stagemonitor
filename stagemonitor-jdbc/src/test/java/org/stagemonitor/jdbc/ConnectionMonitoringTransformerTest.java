package org.stagemonitor.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.requestmonitor.MonitoredMethodRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;

public class ConnectionMonitoringTransformerTest {

	private static Connection connection;
	private TestDataSource dataSource;
	private RequestMonitor requestMonitor;

	@BeforeClass
	public static void attachProfiler() {
		Stagemonitor.init();
	}

	@Before
	public void setUp() throws Exception {
		CorePlugin corePlugin = mock(CorePlugin.class);
		RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		Configuration configuration = mock(Configuration.class);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);

		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		when(corePlugin.getElasticsearchUrls()).thenReturn(Collections.singletonList("http://mockhost"));
		when(requestMonitorPlugin.isCollectRequestStats()).thenReturn(true);
		when(requestMonitorPlugin.getOnlyCollectNCallTreesPerMinute()).thenReturn(1000000d);
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(1000000d);
		when(requestMonitorPlugin.isOnlyLogElasticsearchRequestTraceReports()).thenReturn(true);
		when(requestMonitorPlugin.isProfilerActive()).thenReturn(true);

		Stagemonitor.getMetric2Registry().removeMatching(MetricFilter.ALL);

		dataSource = new TestDataSource();
		connection = mock(Connection.class);
		final DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		when(metaData.getURL()).thenReturn("jdbc:test");
		when(metaData.getUserName()).thenReturn("testUser");
		when(connection.getMetaData()).thenReturn(metaData);

		when(connection.prepareStatement(anyString())).thenReturn(mock(PreparedStatement.class));
		when(connection.createStatement()).thenReturn(mock(Statement.class));
		requestMonitor = new RequestMonitor(configuration, Stagemonitor.getMetric2Registry());
	}

	@AfterClass
	public static void cleanUp() {
		Stagemonitor.reset();
		SharedMetricRegistries.clear();
	}

	@Test
	public void monitorGetConnection() throws Exception {
		dataSource.getConnection();
		final Map<MetricName, Timer> timers = Stagemonitor.getMetric2Registry().getTimers();
		assertNotNull(timers.keySet().toString(), timers.get(name("get_jdbc_connection").tag("url", "jdbc:test-testUser").build()));
	}

	@Test
	public void monitorGetConnectionUsernamePassword() throws Exception {
		dataSource.getConnection("user", "pw");
		final Map<MetricName, Timer> timers = Stagemonitor.getMetric2Registry().getTimers();
		assertNotNull(timers.keySet().toString(), timers.get(name("get_jdbc_connection").tag("url", "jdbc:test-testUser").build()));
	}

	@Test
	public void testRecordSqlPreparedStatement() throws Exception {
		final RequestMonitor.RequestInformation<RequestTrace> requestInformation = requestMonitor
				.monitor(new MonitoredMethodRequest("testRecordSql()", new MonitoredMethodRequest.MethodExecution() {
					@Override
					public Object execute() throws Exception {
						Profiler.start("public void org.stagemonitor.jdbc.ConnectionMonitoringTransformerTest.testRecordSql()");
						dataSource.getConnection().prepareStatement("SELECT * from STAGEMONITOR").execute();
						Profiler.stop();
						return null;
					}
				}));
		final Map<MetricName, Timer> timers = Stagemonitor.getMetric2Registry().getTimers();
		assertTrue(timers.keySet().toString(), timers.size() > 1);
		assertNotNull(timers.keySet().toString(), timers.get(name("jdbc_statement").tag("signature", "All").build()));
		assertNotNull(timers.keySet().toString(), timers.get(name("jdbc_statement").tag("signature", "ConnectionMonitoringTransformerTest#testRecordSql").build()));
		final CallStackElement callStack = requestInformation.getRequestTrace().getCallStack();
		assertEquals("testRecordSql()", callStack.getSignature());
		assertEquals("public void org.stagemonitor.jdbc.ConnectionMonitoringTransformerTest.testRecordSql()",
				callStack.getChildren().get(0).getSignature());
		assertEquals("SELECT * from STAGEMONITOR ", callStack.getChildren().get(0).getChildren().get(0).getSignature());
	}

	@Test
	public void testRecordSqlStatement() throws Exception {
		final RequestMonitor.RequestInformation<RequestTrace> requestInformation = requestMonitor
				.monitor(new MonitoredMethodRequest("testRecordSql()", new MonitoredMethodRequest.MethodExecution() {
					@Override
					public Object execute() throws Exception {
						Profiler.start("public void org.stagemonitor.jdbc.ConnectionMonitoringTransformerTest.testRecordSql()");
						dataSource.getConnection().createStatement().execute("SELECT * from STAGEMONITOR");
						Profiler.stop();
						return null;
					}
				}));
		final Map<MetricName, Timer> timers = Stagemonitor.getMetric2Registry().getTimers();
		assertTrue(timers.keySet().toString(), timers.size() > 1);
		assertNotNull(timers.keySet().toString(), timers.get(name("jdbc_statement").tag("signature", "All").build()));
		assertNotNull(timers.keySet().toString(), timers.get(name("jdbc_statement").tag("signature", "ConnectionMonitoringTransformerTest#testRecordSql").build()));
		final CallStackElement callStack = requestInformation.getRequestTrace().getCallStack();
		assertEquals("testRecordSql()", callStack.getSignature());
		assertEquals("public void org.stagemonitor.jdbc.ConnectionMonitoringTransformerTest.testRecordSql()",
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
