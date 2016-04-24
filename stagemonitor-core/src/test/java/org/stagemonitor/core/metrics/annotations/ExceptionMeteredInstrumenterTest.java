package org.stagemonitor.core.metrics.annotations;

import static org.junit.Assert.assertEquals;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.annotation.ExceptionMetered;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;

public class ExceptionMeteredInstrumenterTest {

	private TestObject testObject = new TestObject();

	private static class TestObject {
		@ExceptionMetered
		private void exceptionMeteredDefault() {
			throw null;
		}

		@ExceptionMetered
		private void exceptionMeteredPrivate() {
			throw null;
		}

		@ExceptionMetered(absolute = true)
		public void exceptionMeteredAbsolute() {
			throw null;
		}

		@ExceptionMetered(name = "myExceptionMeteredName")
		public void exceptionMeteredName() {
			throw null;
		}

		@ExceptionMetered(name = "myExceptionMeteredNameAbsolute", absolute = true)
		public void exceptionMeteredNameAbsolute() {
			throw null;
		}

		@ExceptionMetered(cause = NullPointerException.class, absolute = true)
		public void exceptionMeteredCauseExact() {
			throw null;
		}

		@ExceptionMetered(cause = RuntimeException.class, absolute = true)
		public void exceptionMeteredCauseSubtype() {
			throw null;
		}

		@ExceptionMetered(cause = RuntimeException.class, absolute = true)
		public void exceptionMeteredCauseSupertype() throws Exception {
			throw new Exception();
		}

		@ExceptionMetered(cause = Exception.class)
		public void exceptionMeteredCauseNoException() {
		}

		@ExceptionMetered
		public void exceptionMeteredNoException() {
		}
	}

	@BeforeClass
	public static void attachProfiler() {
		Stagemonitor.init();
	}

	@Before
	@After
	public void clearMetricRegistry() {
		Stagemonitor.getMetric2Registry().removeMatching(MetricFilter.ALL);
	}

	@AfterClass
	public static void resetStagemonitor() {
		Stagemonitor.reset();
		SharedMetricRegistries.clear();
	}

	@Test
	public void testExceptionMeteredAspectDefault() {
		try {
			testObject.exceptionMeteredDefault();
		} catch (Exception e) {
			// ignore
		}
		assertOneMeterExists(name("exception_rate").tag("signature", "ExceptionMeteredInstrumenterTest$TestObject#exceptionMeteredDefault").build());
	}

	@Test
	public void testExceptionMeteredAspectexceptionMeteredPrivate() {
		try {
			testObject.exceptionMeteredPrivate();
		} catch (Exception e) {
			// ignore
		}
		assertEquals(1, Stagemonitor.getMetric2Registry().getMeters().size());
	}

	@Test
	public void testExceptionMeteredAspectAbsolute() {
		try {
			testObject.exceptionMeteredAbsolute();
		} catch (Exception e) {
			// ignore
		}
		assertOneMeterExists(name("exception_rate").tag("signature", "exceptionMeteredAbsolute").build());
	}

	@Test
	public void testExceptionMeteredName() {
		try {
			testObject.exceptionMeteredName();
		} catch (Exception e) {
			// ignore
		}
		assertOneMeterExists(name("exception_rate").tag("signature", "ExceptionMeteredInstrumenterTest$TestObject#myExceptionMeteredName").build());
	}

	@Test
	public void testExceptionMeteredNameAbsolute() {
		try {
			testObject.exceptionMeteredNameAbsolute();
		} catch (Exception e) {
			// ignore
		}
		assertOneMeterExists(name("exception_rate").tag("signature", "myExceptionMeteredNameAbsolute").build());
	}

	@Test
	public void testExceptionMeteredCauseExact() {
		try {
			testObject.exceptionMeteredCauseExact();
		} catch (Exception e) {
			// ignore
		}
		assertOneMeterExists(name("exception_rate").tag("signature", "exceptionMeteredCauseExact").build());
	}

	@Test
	public void testExceptionMeteredCauseSubtype() {
		try {
			testObject.exceptionMeteredCauseSubtype();
		} catch (Exception e) {
			// ignore
		}
		assertOneMeterExists(name("exception_rate").tag("signature", "exceptionMeteredCauseSubtype").build());
	}

	@Test
	public void testExceptionMeteredCauseSupertype() {
		try {
			testObject.exceptionMeteredCauseSupertype();
		} catch (Exception e) {
			// ignore
		}
		final Metric2Registry metricRegistry = Stagemonitor.getMetric2Registry();
		assertEquals(metricRegistry.getMeters().toString(), 0, metricRegistry.getMeters().size());
	}

	@Test
	public void testExceptionMeteredCauseNoException() {
		try {
			testObject.exceptionMeteredCauseNoException();
		} catch (Exception e) {
			// ignore
		}
		final Metric2Registry metricRegistry = Stagemonitor.getMetric2Registry();
		assertEquals(0, metricRegistry.getMeters().size());
	}

	@Test
	public void testExceptionMeteredNoException() {
		try {
			testObject.exceptionMeteredNoException();
		} catch (Exception e) {
			// ignore
		}
		final Metric2Registry metricRegistry = Stagemonitor.getMetric2Registry();
		assertEquals(0, metricRegistry.getMeters().size());
	}

	private void assertOneMeterExists(MetricName timerName) {
		final Metric2Registry metricRegistry = Stagemonitor.getMetric2Registry();
		assertEquals(1, metricRegistry.getMeters().size());
		assertEquals(timerName, metricRegistry.getMeters().keySet().iterator().next());
	}

}
