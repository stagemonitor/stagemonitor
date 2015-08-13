package org.stagemonitor.core.metrics.annotations;

import static org.junit.Assert.assertEquals;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;

public class GaugeInstrumenterTest {

	private GaugeTestObject testObject = new GaugeTestObject();

	@BeforeClass
	@AfterClass
	public static void attachProfiler() {
		Stagemonitor.reset();
	}

	@Test
	public void testGaugeAspectDefault() {
		final Metric2Registry metricRegistry = Stagemonitor.getMetric2Registry();
		assertEquals(5, metricRegistry.getGauges().size());
		Set<MetricName> metricNames = new HashSet<MetricName>();
		metricNames.add(name("gauge_GaugeTestObject#gaugeDefault").build());
		metricNames.add(name("gauge_GaugeTestObject#staticGaugeDefault").build());
		metricNames.add(name("gauge_gaugeAbsolute").build());
		metricNames.add(name("gauge_GaugeTestObject#myGaugeName").build());
		metricNames.add(name("gauge_myGaugeNameAbsolute").build());
		assertEquals(metricNames, metricRegistry.getGauges().keySet());
	}
}
