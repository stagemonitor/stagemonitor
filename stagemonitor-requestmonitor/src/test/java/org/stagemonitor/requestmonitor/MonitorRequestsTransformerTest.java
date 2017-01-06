package org.stagemonitor.requestmonitor;

import com.codahale.metrics.Timer;
import com.uber.jaeger.Span;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.Metric2Filter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.requestmonitor.reporter.LoggingSpanReporter;
import org.stagemonitor.requestmonitor.reporter.SpanReporter;
import org.stagemonitor.requestmonitor.utils.SpanTags;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class MonitorRequestsTransformerTest {

	private TestClass testClass;
	private TestClassLevelAnnotationClass testClassLevelAnnotationClass;
	private Metric2Registry metricRegistry;
	private SpanCapturingReporter requestTraceCapturingReporter;

	@BeforeClass
	public static void attachProfiler() {
		Stagemonitor.init();
	}

	@Before
	public void before() throws Exception {
		testClass = new TestSubClass();
		metricRegistry = Stagemonitor.getMetric2Registry();
		testClassLevelAnnotationClass = new TestClassLevelAnnotationClass();
		metricRegistry.removeMatching(Metric2Filter.ALL);
		Stagemonitor.setMeasurementSession(new MeasurementSession("MonitorRequestsTransformerTest", "test", "test"));
		Stagemonitor.startMonitoring();
		requestTraceCapturingReporter = new SpanCapturingReporter();
	}

	@AfterClass
	public static void resetStagemonitor() {
		Stagemonitor.reset();
	}

	@Test
	public void testMonitorRequests() throws Exception {
		testClass.monitorMe(1);
		final SpanReporter.ReportArguments reportArguments = requestTraceCapturingReporter.get();
		final Span span = (Span) reportArguments.getSpan();
		new LoggingSpanReporter().report(reportArguments);
		// either parameters.arg0 or parameters.s
		assertEquals("1", getTagsStartingWith(reportArguments.getInternalSpan().getTags(), SpanTags.PARAMETERS_PREFIX).iterator().next());
		assertEquals("MonitorRequestsTransformerTest$TestClass#monitorMe", span.getOperationName());
		assertEquals(1, reportArguments.getCallTree().getChildren().size());
		final String signature = reportArguments.getCallTree().getChildren().get(0).getSignature();
		assertTrue(signature, signature.contains("org.stagemonitor.requestmonitor.MonitorRequestsTransformerTest$TestClass.monitorMe"));

		final Map<MetricName,Timer> timers = metricRegistry.getTimers();
		assertNotNull(timers.keySet().toString(), timers.get(name("response_time_server").tag("request_name", "MonitorRequestsTransformerTest$TestClass#monitorMe").layer("All").build()));
	}

	@Test
	public void testDontMonitorAnySuperMethod() throws Exception {
		testClass.dontMonitorMe();
		assertNull(requestTraceCapturingReporter.get());
	}

	@Test
	public void testMonitorRequestsThrowingException() throws Exception {
		try {
			testClass.monitorThrowException();
			fail();
		} catch (NullPointerException e) {
			// expected
		}
		assertEquals(NullPointerException.class.getName(), requestTraceCapturingReporter.getSpan().getTags().get("exception.class"));

		final Map<MetricName,Timer> timers = metricRegistry.getTimers();
		assertNotNull(timers.keySet().toString(), timers.get(name("response_time_server").tag("request_name", "MonitorRequestsTransformerTest$TestClass#monitorThrowException").layer("All").build()));
	}

	@Test
	public void testMonitorRequestsAnnonymousInnerClass() throws Exception {
		testClass.monitorAnnonymousInnerClass();
		final SpanReporter.ReportArguments reportArguments = requestTraceCapturingReporter.get();
		assertEquals("MonitorRequestsTransformerTest$TestClass$1#run", reportArguments.getInternalSpan().getOperationName());
		assertEquals(1, reportArguments.getCallTree().getChildren().size());
		final String signature = reportArguments.getCallTree().getChildren().get(0).getSignature();
		assertTrue(signature, signature.contains("org.stagemonitor.requestmonitor.MonitorRequestsTransformerTest$TestClass$1.run"));

		final Map<MetricName,Timer> timers = metricRegistry.getTimers();
		assertNotNull(timers.keySet().toString(), timers.get(name("response_time_server").tag("request_name", "MonitorRequestsTransformerTest$TestClass$1#run").layer("All").build()));
	}

	@Test
	public void testMonitorRequestsResolvedAtRuntime() throws Exception {
		testClass.resolveNameAtRuntime();
		final String operationName = requestTraceCapturingReporter.getSpan().getOperationName();
		assertEquals("MonitorRequestsTransformerTest$TestSubClass#resolveNameAtRuntime", operationName);
	}

	@Test
	public void testMonitorStaticMethod() throws Exception {
		TestClass.monitorStaticMethod();
		final String operationName = requestTraceCapturingReporter.getSpan().getOperationName();
		assertEquals("MonitorRequestsTransformerTest$TestClass#monitorStaticMethod", operationName);
	}

	@Test
	public void testMonitorRequestsCustomName() throws Exception {
		testClass.doFancyStuff();
		final String operationName = requestTraceCapturingReporter.getSpan().getOperationName();
		assertEquals("My Cool Method", operationName);
	}

	private static abstract class SuperAbstractTestClass {
		@MonitorRequests
		public abstract int monitorMe(int i) throws Exception;
	}

	private static abstract class AbstractTestClass extends SuperAbstractTestClass{
		public abstract void dontMonitorMe() throws Exception;
	}

	private static class TestClass extends AbstractTestClass {
		public int monitorMe(int i) throws Exception {
			return i;
		}

		@Override
		public void dontMonitorMe() throws Exception {
		}

		@MonitorRequests(resolveNameAtRuntime = true)
		public void resolveNameAtRuntime() throws Exception {
		}

		@MonitorRequests
		public static void monitorStaticMethod() {
		}

		@MonitorRequests(requestName = "My Cool Method")
		public void doFancyStuff() throws Exception {
		}

		public void monitorAnnonymousInnerClass() {
			new Runnable() {
				@Override
				@MonitorRequests
				public void run() {

				}
			}.run();
		}

		@MonitorRequests
		public int monitorThrowException() throws Exception {
			throw null;
		}
	}

	private static class TestSubClass extends TestClass {
	}

	@Test
	public void testClassLevelAnnotationClass() throws Exception {
		testClassLevelAnnotationClass.monitorMe("1");
		testClassLevelAnnotationClass.dontMonitorMe();
		final SpanReporter.ReportArguments reportArguments = requestTraceCapturingReporter.get();

		// either parameters.arg0 or parameters.s
		assertEquals("1", getTagsStartingWith(reportArguments.getInternalSpan().getTags(), SpanTags.PARAMETERS_PREFIX).iterator().next());
		assertEquals("MonitorRequestsTransformerTest$TestClassLevelAnnotationClass#monitorMe", reportArguments.getInternalSpan().getOperationName());
		assertEquals(1, reportArguments.getCallTree().getChildren().size());
		final String signature = reportArguments.getCallTree().getChildren().get(0).getSignature();
		assertTrue(signature, signature.contains("org.stagemonitor.requestmonitor.MonitorRequestsTransformerTest$TestClassLevelAnnotationClass.monitorMe"));

		final Map<MetricName, Timer> timers = metricRegistry.getTimers();
		assertNotNull(timers.keySet().toString(), timers.get(name("response_time_server").tag("request_name", "MonitorRequestsTransformerTest$TestClassLevelAnnotationClass#monitorMe").layer("All").build()));
	}

	@MonitorRequests
	private static class SuperTestClassLevelAnnotationClass {
	}

	private static class TestClassLevelAnnotationClass extends SuperTestClassLevelAnnotationClass {

		public String monitorMe(String s) throws Exception {
			return s;
		}

		private int dontMonitorMe() throws Exception {
			return 0;
		}

	}

	private List<Object> getTagsStartingWith(Map<String, Object> tags, String prefix) {
		List<Object> tagValuesStartingWith = new ArrayList<Object>();
		for (Map.Entry<String, Object> entry : tags.entrySet()) {
			if (entry.getKey().startsWith(prefix)) {
				tagValuesStartingWith.add(entry.getValue());
			}
		}
		return tagValuesStartingWith;
	}

}
