package org.stagemonitor.os;

import com.codahale.metrics.MetricRegistry;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.GraphiteSanitizer;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static com.codahale.metrics.MetricRegistry.name;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class must not end with 'Test'
 * Travis build container don't have a exposed network interfance, so this test is excluded for Travis builds (see .travis.yml)
 */
public class OsPluginCheck {

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

	@Test
	public void testGetHostname() {
		assertNotNull(OsPlugin.getHostName());
		assertNotNull(OsPlugin.getHostNameFromEnv());
	}

	@Test
	public void testFileSystemUsage() throws Exception {
		String baseName = getFsBaseName();
		assertEquals(getLongGauge(name(baseName, "usage.total")),
				getLongGauge(name(baseName, "usage.free")) + getLongGauge(name(baseName, "usage.used")));
	}

	private String getFsBaseName() throws SigarException {
		String fsName = "";
		@SuppressWarnings("unchecked")
		final Set<Map.Entry<String, FileSystem>> entries = (Set<Map.Entry<String, FileSystem>>) sigar.getFileSystemMap().entrySet();
		for (Map.Entry<String, FileSystem> e : entries) {
			if (e.getValue().getType() == FileSystem.TYPE_LOCAL_DISK) {
				fsName = e.getKey();
			}
		}
		return name("os.fs", GraphiteSanitizer.sanitizeGraphiteMetricSegment(fsName.replace("\\", "")));
	}

	@Test
	public void testFileSystemMetrics() throws Exception {
		String baseName = getFsBaseName();
		assertTrue(metricRegistry.getGauges().keySet().toString(), getDoubleGauge(name(baseName, "usage-percent")) >= 0);
		assertTrue(getDoubleGauge(name(baseName, "usage-percent")) <= 1);
		assertTrue(getLongGauge(name(baseName, "reads.bytes")) >= 0);
		assertTrue(getLongGauge(name(baseName, "writes.bytes")) >= 0);
		assertTrue(getDoubleGauge(name(baseName, "disk.queue")) >= 0);
	}

	private double getDoubleGauge(String gaugeName) {
		return (Double) getGauge(gaugeName);
	}

	private long getLongGauge(String gaugeName) {
		return (Long) getGauge(gaugeName);
	}

	private Object getGauge(String gaugeName) {
		return metricRegistry.getGauges().get(gaugeName).getValue();
	}

}
