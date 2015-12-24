package org.stagemonitor.core.metrics.metrics2;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistryListener;

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
		Mockito.verify(listener).onGaugeAdded(Mockito.eq("test"), Mockito.any());

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

}
