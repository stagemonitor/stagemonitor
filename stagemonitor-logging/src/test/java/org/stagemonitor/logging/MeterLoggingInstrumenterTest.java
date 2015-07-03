package org.stagemonitor.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.SortedMap;

import com.codahale.metrics.Meter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;

public class MeterLoggingInstrumenterTest {

	private Logger logger;

	@BeforeClass
	public static void attachProfiler() {
		Stagemonitor.init();
	}

	@Before
	@After
	public void reinit() throws Exception {
		Stagemonitor.reset();
		logger = LoggerFactory.getLogger(getClass());
	}

	@Test
	public void testLogging() throws Exception {
		new LoggingPlugin().retransformLogger();

		logger.error("test");
		final SortedMap<String,Meter> meters = Stagemonitor.getMetricRegistry().getMeters();
		assertEquals(1, meters.size());
		assertNotNull(meters.get("logging.error"));
		assertEquals(1, meters.get("logging.error").getCount());
	}
}
