package org.stagemonitor.core.metrics.annotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.annotation.Timed;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.scheduling.annotation.Async;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;

public class TimedInstrumenterTest {

	private TestObject testObject = new TestObject();

	private static class TestObject {
		@Timed
		public void timedDefault() {
		}

		@Timed
		private void timedPrivate() {
		}

		@Timed(absolute = true)
		@Async
		public void timedAbsolute() {
		}

		@Timed(name = "myTimedName")
		public void timedName() {
		}

		@Timed(name = "myTimedNameAbsolute", absolute = true)
		public void timedNameAbsolute() {
		}

		@Async
		public void asyncCall() {
		}
	}

	@BeforeClass
	public static void attachProfiler() {
		Stagemonitor.init();
	}

	@Before
	public void before() {
		Stagemonitor.getMetric2Registry().removeMatching(MetricFilter.ALL);
	}

	@Test
	public void testTimedAspectDefault() {
		testObject.timedDefault();
		assertOneTimerExists(name("timer").tag("signature", "TimedInstrumenterTest$TestObject#timedDefault").build());
	}

	@Test
	public void testTimedAspectPrivate() {
		testObject.timedPrivate();
		assertEquals(1, Stagemonitor.getMetric2Registry().getTimers().size());
	}

	@Test
	public void testTimedAspectAbsolute() {
		testObject.timedAbsolute();
		assertOneTimerExists(name("timer").tag("signature", "timedAbsolute").build());
	}

	@Test
	public void testTimedName() {
		testObject.timedName();
		assertOneTimerExists(name("timer").tag("signature", "TimedInstrumenterTest$TestObject#myTimedName").build());
	}

	@Test
	public void testTimedNameAbsolute() {
		testObject.timedNameAbsolute();
		assertOneTimerExists(name("timer").tag("signature", "myTimedNameAbsolute").build());
	}

	@Test
	public void testAsyncAnnotation() {
		testObject.asyncCall();
		assertOneTimerExists(name("timer").tag("signature", "TimedInstrumenterTest$TestObject#asyncCall").build());
	}

	private void assertOneTimerExists(MetricName name) {
		final Metric2Registry metricRegistry = Stagemonitor.getMetric2Registry();
		assertNotNull(metricRegistry.getTimers().keySet().toString(), metricRegistry.getTimers().get(name));
	}

}
