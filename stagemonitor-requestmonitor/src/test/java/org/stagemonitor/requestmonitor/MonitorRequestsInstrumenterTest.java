package org.stagemonitor.requestmonitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.lang.reflect.Field;
import java.util.Map;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.Timer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.junit.ConditionalTravisTestRunner;
import org.stagemonitor.junit.ExcludeOnTravis;

@RunWith(ConditionalTravisTestRunner.class)
public class MonitorRequestsInstrumenterTest {

	private TestClass testClass;
	private static RequestMonitor.RequestInformation<? extends RequestTrace> requestInformation;
	private Metric2Registry metricRegistry;

	@BeforeClass
	public static void attachProfiler() {
		Stagemonitor.init();
	}

	@Before
	public void before() {
		testClass = new TestClass();
		metricRegistry = Stagemonitor.getMetric2Registry();
		metricRegistry.removeMatching(MetricFilter.ALL);
	}

	@AfterClass
	public static void resetStagemonitor() {
		Stagemonitor.reset();
	}

	@Test
	@ExcludeOnTravis
	public void testMonitorRequests() throws Exception {
		testClass.monitorMe(1);
		assertNotNull(requestInformation);
		final RequestTrace requestTrace = requestInformation.getRequestTrace();
		assertEquals("1", requestTrace.getParameter());
		assertEquals("MonitorRequestsInstrumenterTest$TestClass#monitorMe", requestTrace.getName());
		assertEquals(1, requestTrace.getCallStack().getChildren().size());
		assertEquals("int org.stagemonitor.requestmonitor.MonitorRequestsInstrumenterTest$TestClass.monitorMe(int)", requestTrace.getCallStack().getChildren().get(0).getSignature());
		assertEquals(1, requestTrace.getCallStack().getChildren().get(0).getChildren().size());
		assertEquals("void org.stagemonitor.requestmonitor.MonitorRequestsInstrumenterTest$TestClass.getRequestInformation()", requestTrace.getCallStack().getChildren().get(0).getChildren().get(0).getSignature());

		final Map<MetricName,Timer> timers = metricRegistry.getTimers();
		assertNotNull(timers.keySet().toString(), timers.get(name("response_time").tag("request_name", "MonitorRequestsInstrumenterTest$TestClass#monitorMe").tier("server").layer("total").build()));
	}

	@Test
	@ExcludeOnTravis
	public void testMonitorRequestsThrowingException() throws Exception {
		try {
			testClass.monitorThrowException();
			fail();
		} catch (NullPointerException e) {
			// expected
		}
		assertNotNull(requestInformation);
		final RequestTrace requestTrace = requestInformation.getRequestTrace();
		assertEquals(NullPointerException.class.getName(), requestTrace.getExceptionClass());

		final Map<MetricName,Timer> timers = metricRegistry.getTimers();
		assertNotNull(timers.keySet().toString(), timers.get(name("response_time").tag("request_name", "MonitorRequestsInstrumenterTest$TestClass#monitorThrowException").tier("server").layer("total").build()));
	}

	private static class TestClass {
		@MonitorRequests
		public int monitorMe(int i) throws Exception {
			getRequestInformation();
			return i;
		}

		@MonitorRequests
		public int monitorThrowException() throws Exception {
			getRequestInformation();
			throw null;
		}

		private static void getRequestInformation() throws NoSuchFieldException, IllegalAccessException {
			final Field request = RequestMonitor.class.getDeclaredField("request");
			request.setAccessible(true);
			ThreadLocal<RequestMonitor.RequestInformation<? extends RequestTrace>> requestThreadLocal = (ThreadLocal<RequestMonitor.RequestInformation<? extends RequestTrace>>) request.get(null);
			requestInformation = requestThreadLocal.get();
		}
	}
}
