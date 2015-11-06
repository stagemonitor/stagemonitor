package org.stagemonitor.requestmonitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.annotations.MeteredInstrumenter;
import org.stagemonitor.core.metrics.annotations.TimedInstrumenter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;

public class MultipleAnnotationsAndProfilerTest {

	private TestObject testObject = new TestObject();

	private static class TestObject {
		@Metered
		@Timed
		public void testMethod() {
		}
	}

	@BeforeClass
	public static void attachProfiler() {
		Stagemonitor.init();
	}

	@Before
	@After
	public void clearMetricRegistry() {
		MeteredInstrumenter.init();
		TimedInstrumenter.init();
		Stagemonitor.getMetric2Registry().removeMatching(MetricFilter.ALL);
	}

	@AfterClass
	public static void resetStagemonitor() {
		Stagemonitor.reset();
		SharedMetricRegistries.clear();
	}

	@Test
	public void testMeterTimer() {
		CallStackElement total = Profiler.activateProfiling("total");
		testObject.testMethod();
		Profiler.stop();
		assertEquals("void org.stagemonitor.requestmonitor.MultipleAnnotationsAndProfilerTest$TestObject.testMethod()", total.getChildren().get(0).getSignature());
		assertOneMeterExists(name("rate").tag("signature", "MultipleAnnotationsAndProfilerTest$TestObject#testMethod").build());
		assertOneTimerExists(name("timer").tag("signature", "MultipleAnnotationsAndProfilerTest$TestObject#testMethod").build());
	}

	private void assertOneMeterExists(MetricName name) {
		final Metric2Registry metricRegistry = Stagemonitor.getMetric2Registry();
		assertNotNull(metricRegistry.getMeters().keySet().toString(), metricRegistry.getMeters().get(name));
	}

	private void assertOneTimerExists(MetricName name) {
		final Metric2Registry metricRegistry = Stagemonitor.getMetric2Registry();
		assertNotNull(metricRegistry.getTimers().keySet().toString(), metricRegistry.getTimers().get(name));
	}

}
