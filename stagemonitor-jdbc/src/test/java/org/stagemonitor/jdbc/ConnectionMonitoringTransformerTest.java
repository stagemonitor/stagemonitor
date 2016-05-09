package org.stagemonitor.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.Timer;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.requestmonitor.MonitoredMethodRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;

public class ConnectionMonitoringTransformerTest {

	private Configuration configuration;
	private DataSource dataSource;
	private RequestMonitor requestMonitor;
	private Metric2Registry metric2Registry;
	private TestDao testDao;

	@BeforeClass
	public static void attachProfiler() throws Exception {
		Stagemonitor.startMonitoring(new MeasurementSession("ConnectionMonitoringTransformerTest", "test", "test")).get();
	}

	@Before
	public void setUp() throws Exception {
		metric2Registry = Stagemonitor.getMetric2Registry();
		metric2Registry.removeMatching(MetricFilter.ALL);

		final PoolProperties poolProperties = new PoolProperties();
		poolProperties.setDriverClassName("org.hsqldb.jdbcDriver");
		poolProperties.setUrl("jdbc:hsqldb:mem:test");
		dataSource = new org.apache.tomcat.jdbc.pool.DataSource(poolProperties);
		dataSource.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS STAGEMONITOR (FOO INT)").execute();
		dataSource.getConnection().prepareStatement("INSERT INTO STAGEMONITOR (FOO) VALUES (1)").execute();
		requestMonitor = Stagemonitor.getPlugin(RequestMonitorPlugin.class).getRequestMonitor();
		configuration = Stagemonitor.getConfiguration();
		testDao = new TestDao(dataSource);
	}

	@AfterClass
	public static void cleanUp() {
		Stagemonitor.reset();
		Stagemonitor.getMetric2Registry().removeMatching(MetricFilter.ALL);
	}

	@Test
	public void monitorGetConnection() throws Exception {
		requestMonitor
				.monitor(new MonitoredMethodRequest(configuration, "monitorGetConnectionUsernamePassword()", new MonitoredMethodRequest.MethodExecution() {
					@Override
					public Object execute() throws Exception {
						dataSource.getConnection();
						return null;
					}
				})).getRequestTraceReporterFuture().get();
		final Map<MetricName, Timer> timers = metric2Registry.getTimers();
		assertNotNull(timers.keySet().toString(), timers.get(name("get_jdbc_connection").tag("url", "jdbc:hsqldb:mem:test-SA").build()));
	}

	@Test
	public void monitorGetConnectionUsernamePassword() throws Exception {
		dataSource.getConnection("user", "pw");
		requestMonitor
				.monitor(new MonitoredMethodRequest(configuration, "monitorGetConnectionUsernamePassword()", new MonitoredMethodRequest.MethodExecution() {
					@Override
					public Object execute() throws Exception {
						dataSource.getConnection("user", "pw");
						return null;
					}
				})).getRequestTraceReporterFuture().get();
		final Map<MetricName, Timer> timers = metric2Registry.getTimers();
		assertNotNull(timers.keySet().toString(), timers.get(name("get_jdbc_connection").tag("url", "jdbc:hsqldb:mem:test-SA").build()));
	}

	@Test
	public void testRecordSqlPreparedStatement() throws Exception {
		final RequestMonitor.RequestInformation<RequestTrace> requestInformation = requestMonitor
				.monitor(new MonitoredMethodRequest(configuration, "testRecordSqlPreparedStatement", new MonitoredMethodRequest.MethodExecution() {
					@Override
					public Object execute() throws Exception {
						testDao.executePreparedStatement();
						return null;
					}
				}));
		requestInformation.getRequestTraceReporterFuture().get();
		final Map<MetricName, Timer> timers = metric2Registry.getTimers();
		assertTrue(timers.keySet().toString(), timers.size() > 1);
		assertNotNull(timers.keySet().toString(), timers.get(name("jdbc").tag("method", "SELECT").tag("signature", "All").build()));
		assertNotNull(timers.keySet().toString(), timers.get(name("jdbc").tag("method", "SELECT").tag("signature", "ConnectionMonitoringTransformerTest$TestDao#executePreparedStatement").build()));
		final CallStackElement callStack = requestInformation.getRequestTrace().getCallStack();
		assertEquals("testRecordSqlPreparedStatement", callStack.getSignature());
		assertEquals("void org.stagemonitor.jdbc.ConnectionMonitoringTransformerTest$TestDao.executePreparedStatement()",
				callStack.getChildren().get(0).getChildren().get(0).getSignature());
		assertEquals("SELECT * from STAGEMONITOR ", callStack.getChildren().get(0).getChildren().get(0).getChildren().get(0).getSignature());
	}

	@Test
	public void testRecordSqlStatement() throws Exception {
		final RequestMonitor.RequestInformation<RequestTrace> requestInformation = requestMonitor
				.monitor(new MonitoredMethodRequest(configuration, "testRecordSqlStatement", new MonitoredMethodRequest.MethodExecution() {
					@Override
					public Object execute() throws Exception {
						testDao.executeStatement();
						return null;
					}
				}));
		requestInformation.getRequestTraceReporterFuture().get();
		final Map<MetricName, Timer> timers = metric2Registry.getTimers();
		final String message = timers.keySet().toString();
		assertTrue(message, timers.size() > 1);
		assertEquals(message, 1, timers.get(name("jdbc").tag("method", "SELECT").tag("signature", "ConnectionMonitoringTransformerTest$TestDao#executeStatement").build()).getCount());
		assertEquals(message, 1, timers.get(name("jdbc").tag("method", "SELECT").tag("signature", "All").build()).getCount());
		final CallStackElement callStack = requestInformation.getRequestTrace().getCallStack();
		assertEquals("testRecordSqlStatement", callStack.getSignature());
		assertEquals("void org.stagemonitor.jdbc.ConnectionMonitoringTransformerTest$TestDao.executeStatement()",
				callStack.getChildren().get(0).getChildren().get(0).getSignature());
		assertEquals("SELECT * from STAGEMONITOR ", callStack.getChildren().get(0).getChildren().get(0).getChildren().get(0).getSignature());
	}

	public static class TestDao {

		private final DataSource dataSource;

		public TestDao(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		private void executePreparedStatement() throws SQLException {
			final Connection connection = dataSource.getConnection();
			final PreparedStatement preparedStatement = connection.prepareStatement("SELECT * from STAGEMONITOR");
			preparedStatement.execute();
			final ResultSet resultSet = preparedStatement.getResultSet();
			resultSet.next();
		}

		private void executeStatement() throws SQLException {
			dataSource.getConnection().createStatement().execute("SELECT * from STAGEMONITOR");
		}
	}

}
