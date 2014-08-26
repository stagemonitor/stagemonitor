package org.stagemonitor.core.metrics.aspects;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Timed;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.StageMonitor;

import static org.junit.Assert.assertEquals;

public class TimedAspectTest {

	private TestObject testObject = new TestObject();

	private static class TestObject {
		@Timed
		public void timedDefault() {
		}

		@Timed
		private void timedPrivate() {
		}

		@Timed(absolute = true)
		public void timedAbsolute() {
		}

		@Timed(name = "myTimedName")
		public void timedName() {
		}

		@Timed(name = "myTimedNameAbsolute", absolute = true)
		public void timedNameAbsolute() {
		}
	}

	@Before
	public void before() {
		StageMonitor.getMetricRegistry().removeMatching(MetricFilter.ALL);
	}

	@Test
	public void testTimedAspectDefault() {
		testObject.timedDefault();
		assertOneTimerExists("timer.TimedAspectTest$TestObject.timedDefault");
	}

	@Test
	public void testTimedAspectPrivate() {
		testObject.timedPrivate();
		assertEquals(0, StageMonitor.getMetricRegistry().getTimers().size());
	}

	@Test
	public void testTimedAspectAbsolute() {
		testObject.timedAbsolute();
		assertOneTimerExists("timer.timedAbsolute");
	}

	@Test
	public void testTimedName() {
		testObject.timedName();
		assertOneTimerExists("timer.TimedAspectTest$TestObject.myTimedName");
	}

	@Test
	public void testTimedNameAbsolute() {
		testObject.timedNameAbsolute();
		assertOneTimerExists("timer.myTimedNameAbsolute");
	}

	private void assertOneTimerExists(String name) {
		final MetricRegistry metricRegistry = StageMonitor.getMetricRegistry();
		assertEquals(metricRegistry.getTimers().keySet().toString(), 1, metricRegistry.getTimers().size());
		assertEquals(name, metricRegistry.getTimers().keySet().iterator().next());

	}

}
