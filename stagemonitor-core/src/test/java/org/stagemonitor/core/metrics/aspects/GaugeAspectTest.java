package org.stagemonitor.core.metrics.aspects;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import com.codahale.metrics.MetricRegistry;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.MainStagemonitorClassFileTransformer;

public class GaugeAspectTest {

	private GaugeTestObject testObject = new GaugeTestObject();

	@BeforeClass
	public static void attachProfiler() {
		MainStagemonitorClassFileTransformer.performRuntimeAttachment();
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
