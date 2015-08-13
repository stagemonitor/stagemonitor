package org.stagemonitor.core.metrics.annotations;

import static org.junit.Assert.assertEquals;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.annotation.Metered;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;

public class MeteredInstrumenterTest {

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
		MeteredInstrumenter.init();
		Stagemonitor.getMetric2Registry().removeMatching(MetricFilter.ALL);
	}

	@AfterClass
	public static void resetStagemonitor() {
		Stagemonitor.reset();
	}

	@Test
	public void testMeteredAspectDefault() {
		testObject.meteredDefault();
		assertOneMeterExists(name("rate").tag("signature", "MeteredInstrumenterTest$TestObject#meteredDefault").build());
	}

	@Test
	public void testMeteredAspectPrivate() {
		testObject.meteredPrivate();
		assertEquals(1, Stagemonitor.getMetric2Registry().getMeters().size());
	}

	@Test
	public void testMeteredAspectAbsolute() {
		testObject.meteredAbsolute();
		assertOneMeterExists(name("rate").tag("signature", "meteredAbsolute").build());
	}

	@Test
	public void testMeteredName() {
		testObject.meteredName();
		assertOneMeterExists(name("rate").tag("signature", "MeteredInstrumenterTest$TestObject#myMeteredName").build());
	}

	@Test
	public void testMeteredNameAbsolute() {
		testObject.meteredNameAbsolute();
		assertOneMeterExists(name("rate").tag("signature", "myMeteredNameAbsolute").build());
	}

	private void assertOneMeterExists(MetricName name) {
		final Metric2Registry metricRegistry = Stagemonitor.getMetric2Registry();
		assertEquals(1, metricRegistry.getMeters().size());
		assertEquals(name, metricRegistry.getMeters().keySet().iterator().next());
	}

}
