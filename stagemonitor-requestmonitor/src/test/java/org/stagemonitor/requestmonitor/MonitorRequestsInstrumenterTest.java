package org.stagemonitor.requestmonitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.Collections;
import java.util.Map;

import com.codahale.metrics.Timer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.Metric2Filter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.junit.ConditionalTravisTestRunner;
import org.stagemonitor.junit.ExcludeOnTravis;

@RunWith(ConditionalTravisTestRunner.class)
public class MonitorRequestsInstrumenterTest {

	private TestClass testClass;
	private TestClassLevelAnnotationClass testClassLevelAnnotationClass;
	private Metric2Registry metricRegistry;
	private RequestTraceCapturingReporter requestTraceCapturingReporter = new RequestTraceCapturingReporter();

	@BeforeClass
	public static void attachProfiler() {
		Stagemonitor.init();
	}

	@Before
	public void before() throws Exception {
		testClass = new TestClass();
		metricRegistry = Stagemonitor.getMetric2Registry();
		testClassLevelAnnotationClass = new TestClassLevelAnnotationClass();
		metricRegistry.removeMatching(Metric2Filter.ALL);
		Stagemonitor.setMeasurementSession(new MeasurementSession("MonitorRequestsInstrumenterTest", "test", "test"));
		Stagemonitor.startMonitoring().get();
	}

	@AfterClass
	public static void resetStagemonitor() {
		Stagemonitor.reset();
	}

	@Test
	@ExcludeOnTravis
	public void testMonitorRequests() throws Exception {
		testClass.monitorMe(1);
		final RequestTrace requestTrace = requestTraceCapturingReporter.get();
		assertEquals(Collections.singletonMap("0", "1"), requestTrace.getParameters());
		assertEquals("MonitorRequestsInstrumenterTest$TestClass#monitorMe", requestTrace.getName());
		assertEquals(1, requestTrace.getCallStack().getChildren().size());
		final String signature = requestTrace.getCallStack().getChildren().get(0).getSignature();
		assertTrue(signature, signature.contains("org.stagemonitor.requestmonitor.MonitorRequestsInstrumenterTest$TestClass.monitorMe"));

		final Map<MetricName,Timer> timers = metricRegistry.getTimers();
		assertNotNull(timers.keySet().toString(), timers.get(name("response_time_server").tag("request_name", "MonitorRequestsInstrumenterTest$TestClass#monitorMe").layer("All").build()));
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
		final RequestTrace requestTrace = requestTraceCapturingReporter.get();
		assertEquals(NullPointerException.class.getName(), requestTrace.getExceptionClass());

		final Map<MetricName,Timer> timers = metricRegistry.getTimers();
		assertNotNull(timers.keySet().toString(), timers.get(name("response_time_server").tag("request_name", "MonitorRequestsInstrumenterTest$TestClass#monitorThrowException").layer("All").build()));
	}

	private static class TestClass {
		@MonitorRequests
		public int monitorMe(int i) throws Exception {
			return i;
		}

		@MonitorRequests
		public int monitorThrowException() throws Exception {
			throw null;
		}
	}

	@Test
	@ExcludeOnTravis
	public void testClassLevelAnnotationClass() throws Exception {
		testClassLevelAnnotationClass.monitorMe(1);
		testClassLevelAnnotationClass.dontMonitorMe();
		final RequestTrace requestTrace = requestTraceCapturingReporter.get();
		assertEquals(Collections.singletonMap("0", "1"), requestTrace.getParameters());
		assertEquals("MonitorRequestsInstrumenterTest$TestClassLevelAnnotationClass#monitorMe", requestTrace.getName());
		assertEquals(1, requestTrace.getCallStack().getChildren().size());
		final String signature = requestTrace.getCallStack().getChildren().get(0).getSignature();
		assertTrue(signature, signature.contains("org.stagemonitor.requestmonitor.MonitorRequestsInstrumenterTest$TestClassLevelAnnotationClass.monitorMe"));

		final Map<MetricName, Timer> timers = metricRegistry.getTimers();
		assertNotNull(timers.keySet().toString(), timers.get(name("response_time_server").tag("request_name", "MonitorRequestsInstrumenterTest$TestClassLevelAnnotationClass#monitorMe").layer("All").build()));
	}


	@MonitorRequests
	private static class TestClassLevelAnnotationClass {

		public int monitorMe(int i) throws Exception {
			return i;
		}

		private int dontMonitorMe() throws Exception {
			return 0;
		}
	}

}
