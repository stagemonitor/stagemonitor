package org.stagemonitor.core.metrics.aspects;

import static org.junit.Assert.assertEquals;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.annotation.Metered;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;

public class MeteredAspectTest {

	private TestObject testObject = new TestObject();

	private static class TestObject {
		@Metered
		public void meteredDefault() {
		}

		@Metered
		private void meteredPrivate() {
		}

		@Metered(absolute = true)
		public void meteredAbsolute() {
		}

		@Metered(name = "myMeteredName")
		public void meteredName() {
		}

		@Metered(name = "myMeteredNameAbsolute", absolute = true)
		public void meteredNameAbsolute() {
		}
	}

	@BeforeClass
	public static void attachProfiler() {
		Stagemonitor.init();
	}

	@Before
	@After
	public void clearMetricRegistry() {
		Stagemonitor.getMetricRegistry().removeMatching(MetricFilter.ALL);
	}

	@AfterClass
	public static void resetStagemonitor() {
		Stagemonitor.reset();
		SharedMetricRegistries.clear();
	}

	@Test
	public void testMeteredAspectDefault() {
		testObject.meteredDefault();
		assertOneTimerExists("meter.MeteredAspectTest$TestObject#meteredDefault");
	}

	@Test
	public void testMeteredAspectPrivate() {
		testObject.meteredPrivate();
		assertEquals(1, Stagemonitor.getMetricRegistry().getMeters().size());
	}

	@Test
	public void testMeteredAspectAbsolute() {
		testObject.meteredAbsolute();
		assertOneTimerExists("meter.meteredAbsolute");
	}

	@Test
	public void testMeteredName() {
		testObject.meteredName();
		assertOneTimerExists("meter.MeteredAspectTest$TestObject#myMeteredName");
	}

	@Test
	public void testMeteredNameAbsolute() {
		testObject.meteredNameAbsolute();
		assertOneTimerExists("meter.myMeteredNameAbsolute");
	}

	private void assertOneTimerExists(String name) {
		final MetricRegistry metricRegistry = Stagemonitor.getMetricRegistry();
		assertEquals(1, metricRegistry.getMeters().size());
		assertEquals(name, metricRegistry.getMeters().keySet().iterator().next());

	}

}
