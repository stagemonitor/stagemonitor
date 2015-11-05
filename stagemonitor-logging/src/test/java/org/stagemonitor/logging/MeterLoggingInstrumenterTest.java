package org.stagemonitor.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.Map;

import com.codahale.metrics.Meter;
import org.junit.AfterClass;
import com.codahale.metrics.SharedMetricRegistries;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.MetricName;

public class MeterLoggingInstrumenterTest {

	private Logger logger;

	@BeforeClass
	public static void attachProfiler() {
		Stagemonitor.init();
	}

	@Before
	public void reinit() throws Exception {
		Stagemonitor.reset();
		SharedMetricRegistries.clear();
		logger = LoggerFactory.getLogger(getClass());
	}

	@AfterClass
	public static void clear() {
		Stagemonitor.reset();
	}

	@Test
	public void testLogging() throws Exception {
		logger.error("test");
		final Map<MetricName, Meter> meters = Stagemonitor.getMetric2Registry().getMeters();
		assertNotNull(meters.get(name("logging").tag("log_level", "error").build()));
		assertEquals(1, meters.get(name("logging").tag("log_level", "error").build()).getCount());
	}
}
