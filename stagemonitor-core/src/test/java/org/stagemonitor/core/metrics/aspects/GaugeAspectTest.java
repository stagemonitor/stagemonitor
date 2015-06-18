package org.stagemonitor.core.metrics.aspects;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import com.codahale.metrics.MetricRegistry;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;

public class GaugeAspectTest {

	private GaugeTestObject testObject = new GaugeTestObject();

	@BeforeClass
	public static void attachProfiler() {
		Stagemonitor.init();
	}

	@Test
	public void testGaugeAspectDefault() {
		final MetricRegistry metricRegistry = Stagemonitor.getMetricRegistry();
		assertEquals(5, metricRegistry.getGauges().size());
		Set<String> metricNames = new HashSet<String>();
		metricNames.add("gauge.GaugeTestObject#gaugeDefault");
		metricNames.add("gauge.GaugeTestObject#staticGaugeDefault");
		metricNames.add("gauge.gaugeAbsolute");
		metricNames.add("gauge.GaugeTestObject#myGaugeName");
		metricNames.add("gauge.myGaugeNameAbsolute");
		assertEquals(metricNames, metricRegistry.getGauges().keySet());
	}
}
