package org.stagemonitor.core.metrics.aspects;

import static org.junit.Assert.assertEquals;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Timed;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.MainStagemonitorClassFileTransformer;

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

	@BeforeClass
	public static void attachProfiler() {
		MainStagemonitorClassFileTransformer.performRuntimeAttachment();
	}

	@Before
	public void before() {
		Stagemonitor.getMetricRegistry().removeMatching(MetricFilter.ALL);
	}

	@Test
	public void testTimedAspectDefault() {
		testObject.timedDefault();
		assertOneTimerExists("timer.TimedAspectTest$TestObject#timedDefault");
	}

	@Test
	public void testTimedAspectPrivate() {
		testObject.timedPrivate();
		assertEquals(1, Stagemonitor.getMetricRegistry().getTimers().size());
	}

	@Test
	public void testTimedAspectAbsolute() {
		testObject.timedAbsolute();
		assertOneTimerExists("timer.timedAbsolute");
	}

	@Test
	public void testTimedName() {
		testObject.timedName();
		assertOneTimerExists("timer.TimedAspectTest$TestObject#myTimedName");
	}

	@Test
	public void testTimedNameAbsolute() {
		testObject.timedNameAbsolute();
		assertOneTimerExists("timer.myTimedNameAbsolute");
	}

	private void assertOneTimerExists(String name) {
		final MetricRegistry metricRegistry = Stagemonitor.getMetricRegistry();
		assertEquals(metricRegistry.getTimers().keySet().toString(), 1, metricRegistry.getTimers().size());
		assertEquals(name, metricRegistry.getTimers().keySet().iterator().next());

	}

}
