package org.stagemonitor.jdbc;

import com.alibaba.druid.pool.DruidDataSource;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.Timer;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.p6spy.engine.spy.P6DataSource;
import com.zaxxer.hikari.HikariDataSource;

import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.requestmonitor.MonitoredMethodRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;
import java.util.Map;

import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

@RunWith(Parameterized.class)
public class ConnectionMonitoringTransformerTest {

	private static final String DRIVER_CLASS_NAME = "org.hsqldb.jdbcDriver";
	private static final String URL = "jdbc:hsqldb:mem:test";

	private Configuration configuration;
	private DataSource dataSource;
	private RequestMonitor requestMonitor;
	private Metric2Registry metric2Registry;
	private TestDao testDao;

	@Parameterized.Parameters(name = "{index}: {1}")
	public static Iterable<Object[]> data() throws Exception {
		final PoolProperties poolProperties = new PoolProperties();
		poolProperties.setDriverClassName(DRIVER_CLASS_NAME);
		poolProperties.setUrl(URL);
		final org.apache.tomcat.jdbc.pool.DataSource tomcatDataSource = new org.apache.tomcat.jdbc.pool.DataSource(poolProperties);

		ComboPooledDataSource comboPooledDataSource = new ComboPooledDataSource();
		comboPooledDataSource.setDriverClass(DRIVER_CLASS_NAME);
		comboPooledDataSource.setJdbcUrl(URL);

		HikariDataSource hikariDataSource = new HikariDataSource();
		hikariDataSource.setDriverClassName(DRIVER_CLASS_NAME);
		hikariDataSource.setJdbcUrl(URL);

		org.apache.commons.dbcp.BasicDataSource dbcp = new org.apache.commons.dbcp.BasicDataSource();
		dbcp.setDriverClassName(DRIVER_CLASS_NAME);
		dbcp.setUrl(URL);

		org.apache.commons.dbcp2.BasicDataSource dbcp2 = new org.apache.commons.dbcp2.BasicDataSource();
		dbcp2.setDriverClassName(DRIVER_CLASS_NAME);
		dbcp2.setUrl(URL);

		DruidDataSource druidDataSource = new DruidDataSource();
		druidDataSource.setDriverClassName(DRIVER_CLASS_NAME);
		druidDataSource.setUrl(URL);
		druidDataSource.setTestWhileIdle(false);

		return Arrays.asList(new Object[][]{
				{tomcatDataSource, tomcatDataSource.getClass()},
				{comboPooledDataSource, comboPooledDataSource.getClass()},
				{hikariDataSource, hikariDataSource.getClass()},
				{dbcp, dbcp.getClass()},
				{dbcp2, dbcp2.getClass()},
				{new P6DataSource(druidDataSource), druidDataSource.getClass()}
		});
	}

	public ConnectionMonitoringTransformerTest(DataSource dataSource, Class<? extends DataSource> dataSourceClass) {
		this.dataSource = dataSource;
	}

	@BeforeClass
	public static void attachProfiler() throws Exception {
		Stagemonitor.startMonitoring(new MeasurementSession("ConnectionMonitoringTransformerTest", "test", "test"));
	}

	@Before
	public void setUp() throws Exception {
		metric2Registry = Stagemonitor.getMetric2Registry();
		metric2Registry.removeMatching(MetricFilter.ALL);

		try (final Connection connection = dataSource.getConnection()) {
			connection.prepareStatement("CREATE TABLE IF NOT EXISTS STAGEMONITOR (FOO INT)").execute();
			connection.prepareStatement("INSERT INTO STAGEMONITOR (FOO) VALUES (1)").execute();
		}
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
				.monitor(new MonitoredMethodRequest(configuration, "monitorGetConnectionUsernamePassword()", () -> {
					dataSource.getConnection().close();
				}));
		final Map<MetricName, Timer> timers = metric2Registry.getTimers();
		assertNotNull(timers.keySet().toString(), timers.get(name("get_jdbc_connection").tag("url", "SA@jdbc:hsqldb:mem:test").build()));
	}

	@Test
	public void monitorGetConnectionUsernamePassword() throws Exception {
		try {
			dataSource.getConnection("sa", "").close();
		} catch (SQLFeatureNotSupportedException | UnsupportedOperationException e) {
			// ignore
			return;
		}
		requestMonitor
				.monitor(new MonitoredMethodRequest(configuration, "monitorGetConnectionUsernamePassword()", () -> {
					dataSource.getConnection("sa", "").close();
				}));
		final Map<MetricName, Timer> timers = metric2Registry.getTimers();
		assertNotNull(timers.keySet().toString(), timers.get(name("get_jdbc_connection").tag("url", "SA@jdbc:hsqldb:mem:test").build()));
	}

	@Test
	public void testRecordSqlPreparedStatement() throws Exception {
		final SpanContextInformation spanContext = requestMonitor
				.monitor(new MonitoredMethodRequest(configuration, "testRecordSqlPreparedStatement", () -> testDao.executePreparedStatement()));
		final Map<MetricName, Timer> timers = metric2Registry.getTimers();
		assertTrue(timers.keySet().toString(), timers.size() > 1);
		assertNotNull(timers.keySet().toString(), timers.get(name("external_request_response_time").type("jdbc").tag("signature", "All").tag("method", "SELECT").build()));
		assertNotNull(timers.keySet().toString(), timers.get(name("external_request_response_time").type("jdbc").tag("signature", "ConnectionMonitoringTransformerTest$TestDao#executePreparedStatement").tag("method", "SELECT").build()));
		final Map<MetricName, Meter> meters = metric2Registry.getMeters();
		assertNotNull(meters.keySet().toString(), meters.get(name("external_requests_rate").tag("request_name", "testRecordSqlPreparedStatement").tag("type", "jdbc").build()));
		final CallStackElement callTree = spanContext.getCallTree();
		assertEquals("testRecordSqlPreparedStatement", callTree.getSignature());
		assertEquals(callTree.toString(), 1, callTree.getChildren().size());
		assertEquals("void org.stagemonitor.jdbc.ConnectionMonitoringTransformerTest$TestDao.executePreparedStatement()",
				callTree.getChildren().get(0).getSignature());
		assertEquals(callTree.toString(), "SELECT * from STAGEMONITOR ", callTree.getChildren().get(0).getChildren().get(0).getSignature());
	}

	@Test
	public void testRecordSqlStatement() throws Exception {
		final SpanContextInformation spanContext = requestMonitor
				.monitor(new MonitoredMethodRequest(configuration, "testRecordSqlStatement", () -> {
					testDao.executeStatement();
				}));
		final Map<MetricName, Timer> timers = metric2Registry.getTimers();
		final String message = timers.keySet().toString();
		assertTrue(message, timers.size() > 1);
		assertEquals(message, 1, timers.get(name("external_request_response_time").type("jdbc").tag("signature", "ConnectionMonitoringTransformerTest$TestDao#executeStatement").tag("method", "SELECT").build()).getCount());
		assertEquals(message, 1, timers.get(name("external_request_response_time").type("jdbc").tag("signature", "All").tag("method", "SELECT").build()).getCount());
		final CallStackElement callStack = spanContext.getCallTree();
		assertEquals("testRecordSqlStatement", callStack.getSignature());
		assertEquals("void org.stagemonitor.jdbc.ConnectionMonitoringTransformerTest$TestDao.executeStatement()",
				callStack.getChildren().get(0).getSignature());
		assertEquals("SELECT * from STAGEMONITOR ", callStack.getChildren().get(0).getChildren().get(0).getSignature());
	}

	public static class TestDao {

		private final DataSource dataSource;

		public TestDao(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		private void executePreparedStatement() throws SQLException {
			try (final Connection connection = dataSource.getConnection()) {
				final PreparedStatement preparedStatement = connection.prepareStatement("SELECT * from STAGEMONITOR");
				preparedStatement.execute();
				final ResultSet resultSet = preparedStatement.getResultSet();
				resultSet.next();
			}
		}

		private void executeStatement() throws SQLException {
			try (final Connection connection = dataSource.getConnection()) {
				connection.createStatement().execute("SELECT * from STAGEMONITOR");
			}
		}
	}

}
