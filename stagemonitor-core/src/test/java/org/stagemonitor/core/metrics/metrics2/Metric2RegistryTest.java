package org.stagemonitor.core.metrics.metrics2;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistryListener;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Test;
import org.mockito.Mockito;

public class Metric2RegistryTest {
	
	@Test
	public void testWrappingDropwizardMetrics() {
		Metric2Registry registry = new Metric2Registry();
		MetricRegistryListener listener = Mockito.mock(MetricRegistryListener.class);
		registry.getMetricRegistry().addListener(listener);
		
		registry.register(MetricName.name("test").build(), new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return 1;
			}
		});
		
		// Verify the underlying Dropwizard listener was called
		Mockito.verify(listener).onGaugeAdded(Mockito.eq("test"), Mockito.<Gauge>any());

		// Should be able to read the gauge from either registry
		Map.Entry<MetricName, Gauge> stagemonitorEntry = registry.getGauges().entrySet().iterator().next();
		Map.Entry<String, Gauge> dropwizardEntry = registry.getMetricRegistry().getGauges().entrySet().iterator().next();
		assertEquals("test", stagemonitorEntry.getKey().getName());
		assertEquals(1,
				stagemonitorEntry.getValue().getValue());
		
		assertEquals("test", dropwizardEntry.getKey());
		assertEquals(1,
				dropwizardEntry.getValue().getValue());
		
		// Unregister should notify Dropwizard listeners
		registry.remove(MetricName.name("test").build());
		Mockito.verify(listener).onGaugeRemoved(Mockito.eq("test"));
		
		assertEquals(0, registry.getGauges().size());
		assertEquals(0, registry.getMetricRegistry().getGauges().size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDuplicateMetricNameEndsWithIllegalArgumentException() {
		Metric2Registry registry = new Metric2Registry();
		MetricName metric1 = MetricName.name("test").build();
		registry.register(metric1, getGauge());
		registry.register(metric1, getMetered());
	}
	
	@Test
	public void testRegisterAny() {
		Metric2Registry registry = new Metric2Registry();
		final MetricName     metric1 = MetricName.name("test").build();
		final Gauge<Integer> gauge   = getGauge();
		final Metered        metered = getMetered();
		
		Metric2Set metricSet1 = new Metric2Set() {
			@Override
			public Map<MetricName, Metric> getMetrics() {
				Map<MetricName, Metric> map = new HashMap<MetricName, Metric>(1);
				map.put(metric1, gauge);
				return map;
			}
		};
		Metric2Set metricSet2 = new Metric2Set() {
			@Override
			public Map<MetricName, Metric> getMetrics() {
				Map<MetricName, Metric> map = new HashMap<MetricName, Metric>(1);
				map.put(metric1, metered);
				return map;
			}
		};
		registry.registerAny(metricSet1);
		registry.registerAny(metricSet2);
		
		// first one is registered only
		assertEquals(1, registry.getGauges().entrySet().size());
		assertEquals(0, registry.getMeters().entrySet().size());
		Map.Entry<MetricName, Gauge> stagemonitorEntry = registry.getGauges().entrySet().iterator().next();
		assertEquals("test", stagemonitorEntry.getKey().getName());
		assertEquals(1, 		 stagemonitorEntry.getValue().getValue());
	}

	
	//
	// Helper ones
	//
	
	private Metered		getMetered() {
		return new Metered() {
			@Override
			public long getCount() {
				return 1;
			}

			@Override
			public double getFifteenMinuteRate() {
				return 1;
			}

			@Override
			public double getFiveMinuteRate() {
				return 1;
			}

			@Override
			public double getMeanRate() {
				return 1;
			}

			@Override
			public double getOneMinuteRate() {
				return 1;
			}
		};
	}
	
	private Gauge		getGauge() {
		return new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return 1;
			}
		};
	}
	
}