package org.stagemonitor.core.metrics.prometheus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.io.IOException;
import java.util.Arrays;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import io.prometheus.client.CollectorRegistry;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

public class StagemonitorPrometheusCollectorTest {

	private CollectorRegistry registry = new CollectorRegistry();
	private Metric2Registry metricRegistry;

	@Before
	public void setUp() {
		metricRegistry = new Metric2Registry();
		new StagemonitorPrometheusCollector(metricRegistry).register(registry);
	}

	@Test
	public void testCounter() {
		metricRegistry.counter(name("foo_bar").tag("baz", "qux").build()).inc();
		assertEquals(new Double(1),
				registry.getSampleValue("foo_bar", new String[]{"baz"}, new String[]{"qux"})
		);
	}

	@Test
	public void testGauge() {
		Gauge<Integer> integerGauge = new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return 1234;
			}
		};
		Gauge<Double> doubleGauge = new Gauge<Double>() {
			@Override
			public Double getValue() {
				return 1.234D;
			}
		};
		Gauge<Long> longGauge = new Gauge<Long>() {
			@Override
			public Long getValue() {
				return 1234L;
			}
		};
		Gauge<Float> floatGauge = new Gauge<Float>() {
			@Override
			public Float getValue() {
				return 0.1234F;
			}
		};
		Gauge<Boolean> booleanGauge = new Gauge<Boolean>() {
			@Override
			public Boolean getValue() {
				return true;
			}
		};

		metricRegistry.register(name("double_gauge").tag("foo", "bar").build(), doubleGauge);
		metricRegistry.register(name("long_gauge").tag("foo", "bar").build(), longGauge);
		metricRegistry.register(name("integer_gauge").tag("foo", "bar").build(), integerGauge);
		metricRegistry.register(name("float_gauge").tag("foo", "bar").build(), floatGauge);
		metricRegistry.register(name("boolean_gauge").tag("foo", "bar").build(), booleanGauge);

		assertEquals(new Double(1234),
				registry.getSampleValue("integer_gauge", new String[]{"foo"}, new String[]{"bar"}));
		assertEquals(new Double(1234),
				registry.getSampleValue("long_gauge", new String[]{"foo"}, new String[]{"bar"}));
		assertEquals(new Double(1.234),
				registry.getSampleValue("double_gauge", new String[]{"foo"}, new String[]{"bar"}));
		assertEquals(new Double(0.1234F),
				registry.getSampleValue("float_gauge", new String[]{"foo"}, new String[]{"bar"}));
		assertEquals(new Double(1),
				registry.getSampleValue("boolean_gauge", new String[]{"foo"}, new String[]{"bar"}));
	}

	@Test
	public void testInvalidGaugeType() {
		Gauge<String> invalidGauge = new Gauge<String>() {
			@Override
			public String getValue() {
				return "foobar";
			}
		};
		metricRegistry.register(name("invalid_gauge").build(), invalidGauge);
		assertEquals(null, registry.getSampleValue("invalid_gauge"));
	}

	@Test
	public void testHistogram() throws IOException {
		Histogram hist = metricRegistry.histogram(name("hist").build());
		int i = 0;
		while (i < 100) {
			hist.update(i);
			i += 1;
		}
		assertEquals(new Double(100), registry.getSampleValue("hist_count"));
		for (String s : Arrays.asList("0.75", "0.95", "0.98", "0.99")) {
			assertEquals(Double.valueOf((Double.valueOf(s) - 0.01) * 100), registry.getSampleValue("hist",
					new String[]{"quantile"}, new String[]{s}));
		}
		assertEquals(new Double(99), registry.getSampleValue("hist", new String[]{"quantile"},
				new String[]{"0.999"}));
	}

	@Test
	public void testMeter() throws IOException, InterruptedException {
		Meter meter = metricRegistry.meter(name("meter").build());
		meter.mark();
		meter.mark();
		assertEquals(new Double(2), registry.getSampleValue("meter_total"));
	}

	@Test
	public void testTimer() throws IOException, InterruptedException {
		Timer t = metricRegistry.timer(name("timer").tag("foo", "bar").build());
		Timer.Context time = t.time();
		Thread.sleep(1L);
		time.stop();
		// We slept for 1Ms so we ensure that all timers are above 1ms:
		assertTrue(registry.getSampleValue("timer", new String[]{"foo", "quantile"}, new String[]{"bar", "0.99"}) > 1000000);
		assertEquals(Double.valueOf(1.0D), registry.getSampleValue("timer_count", new String[]{"foo"}, new String[]{"bar"}));
		assertNotNull("Metric timer_count should exist", registry.getSampleValue("timer_count", new String[]{"foo"}, new String[]{"bar"}));
	}

}