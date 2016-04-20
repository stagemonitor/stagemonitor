package org.stagemonitor.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.Map;

import javax.sql.DataSource;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.requestmonitor.MonitoredMethodRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;

public class ConnectionMonitoringTransformerTest {

	private DataSource dataSource;
	private RequestMonitor requestMonitor;

	@BeforeClass
	public static void attachProfiler() {
		Stagemonitor.init();
	}

	@Before
	public void setUp() throws Exception {
		Stagemonitor.getMetric2Registry().removeMatching(MetricFilter.ALL);

		final PoolProperties poolProperties = new PoolProperties();
		poolProperties.setDriverClassName("org.hsqldb.jdbcDriver");
		poolProperties.setUrl("jdbc:hsqldb:mem:test");
		dataSource = new org.apache.tomcat.jdbc.pool.DataSource(poolProperties);
		dataSource.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS STAGEMONITOR (FOO INT)").execute();

		requestMonitor = Stagemonitor.getPlugin(RequestMonitorPlugin.class).getRequestMonitor();
	}

	@AfterClass
	public static void cleanUp() {
		Stagemonitor.reset();
		SharedMetricRegistries.clear();
	}

	@Test
	public void monitorGetConnection() throws Exception {
		requestMonitor
				.monitor(new MonitoredMethodRequest("monitorGetConnectionUsernamePassword()", new MonitoredMethodRequest.MethodExecution() {
					@Override
					public Object execute() throws Exception {
						dataSource.getConnection();
						return null;
					}
				}));
		final Map<MetricName, Timer> timers = Stagemonitor.getMetric2Registry().getTimers();
		assertNotNull(timers.keySet().toString(), timers.get(name("get_jdbc_connection").tag("url", "jdbc:hsqldb:mem:test-SA").build()));
	}

	@Test
	public void monitorGetConnectionUsernamePassword() throws Exception {
		dataSource.getConnection("user", "pw");
		requestMonitor
				.monitor(new MonitoredMethodRequest("monitorGetConnectionUsernamePassword()", new MonitoredMethodRequest.MethodExecution() {
					@Override
					public Object execute() throws Exception {
						dataSource.getConnection("user", "pw");
						return null;
					}
				}));
		final Map<MetricName, Timer> timers = Stagemonitor.getMetric2Registry().getTimers();
		assertNotNull(timers.keySet().toString(), timers.get(name("get_jdbc_connection").tag("url", "jdbc:hsqldb:mem:test-SA").build()));
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

}
