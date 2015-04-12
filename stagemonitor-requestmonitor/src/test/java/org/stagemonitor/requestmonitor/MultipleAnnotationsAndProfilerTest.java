package org.stagemonitor.requestmonitor;

import static org.junit.Assert.assertEquals;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
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

	@Before
	@After
	public void clearMetricRegistry() {
		Stagemonitor.getMetricRegistry().removeMatching(MetricFilter.ALL);
	}

	@AfterClass
	public static void resetStagemonitor() {
		Stagemonitor.reset();
	}

	@Test
	public void testMeterTimer() {
		CallStackElement total = Profiler.activateProfiling("total");
		testObject.testMethod();
		Profiler.stop();
		assertEquals("void org.stagemonitor.requestmonitor.MultipleAnnotationsAndProfilerTest$TestObject.testMethod()", total.getChildren().get(0).getSignature());
		assertOneMeterExists("meter.MultipleAnnotationsAndProfilerTest$TestObject#testMethod");
		assertOneTimerExists("timer.MultipleAnnotationsAndProfilerTest$TestObject#testMethod");
	}

	private void assertOneMeterExists(String name) {
		final MetricRegistry metricRegistry = Stagemonitor.getMetricRegistry();
		assertEquals(1, metricRegistry.getMeters().size());
		assertEquals(name, metricRegistry.getMeters().keySet().iterator().next());

	}

	private void assertOneTimerExists(String name) {
		final MetricRegistry metricRegistry = Stagemonitor.getMetricRegistry();
		assertEquals(1, metricRegistry.getTimers().size());
		assertEquals(name, metricRegistry.getTimers().keySet().iterator().next());

	}

}
