package org.stagemonitor.core.metrics.annotations;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

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
		assertThat(metricRegistry.getGauges().keySet()).contains(
				name("gauge_GaugeTestObject#gaugeDefault").build(),
				name("gauge_GaugeTestObject#staticGaugeDefault").build(),
				name("gauge_gaugeAbsolute").build(),
				name("gauge_GaugeTestObject#myGaugeName").build(),
				name("gauge_myGaugeNameAbsolute").build()
		);
	}
}
