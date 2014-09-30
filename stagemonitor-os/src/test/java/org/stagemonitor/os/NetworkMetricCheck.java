package org.stagemonitor.os;

import com.codahale.metrics.MetricRegistry;
import org.hyperic.sigar.Sigar;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.GraphiteSanitizer;

import java.io.IOException;

import static com.codahale.metrics.MetricRegistry.name;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class must not end with 'Test'
 * Travis build container don't have a exposed network interfance, so this test is excluded for Travis builds (see .travis.yml)
 */
public class NetworkMetricCheck {

	private final MetricRegistry metricRegistry = new MetricRegistry();
	private static Sigar sigar;

	static {
		try {
			sigar = OsPlugin.newSigar();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Before
	public void setUp() throws Exception {
		OsPlugin osPlugin = new OsPlugin(sigar);
		final Configuration configuration = mock(Configuration.class);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(mock(CorePlugin.class));
		osPlugin.initializePlugin(metricRegistry, configuration);
	}

	@Test
	public void testNetworkMetrics() throws Exception {
		String baseName = name("os.net", GraphiteSanitizer.sanitizeGraphiteMetricSegment(sigar.getNetInterfaceList()[0]));

		assertTrue(getLongGauge(name(baseName, "read.bytes")) >= 0);
		assertTrue(getLongGauge(name(baseName, "read.packets")) >= 0);
		assertTrue(getLongGauge(name(baseName, "read.errors")) >= 0);
		assertTrue(getLongGauge(name(baseName, "read.dropped")) >= 0);

		assertTrue(getLongGauge(name(baseName, "write.bytes")) >= 0);
		assertTrue(getLongGauge(name(baseName, "write.packets")) >= 0);
		assertTrue(getLongGauge(name(baseName, "write.errors")) >= 0);
		assertTrue(getLongGauge(name(baseName, "write.dropped")) >= 0);
	}

	private long getLongGauge(String gaugeName) {
		return (Long) metricRegistry.getGauges().get(gaugeName).getValue();
	}

}
