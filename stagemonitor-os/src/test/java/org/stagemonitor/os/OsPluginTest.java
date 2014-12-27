package org.stagemonitor.os;

import com.codahale.metrics.MetricRegistry;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.GraphiteSanitizer;
import org.stagemonitor.junit.ConditionalTravisTestRunner;
import org.stagemonitor.junit.ExcludeOnTravis;

import java.util.Map;
import java.util.Set;

import static com.codahale.metrics.MetricRegistry.name;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(ConditionalTravisTestRunner.class)
public class OsPluginTest {

	private final MetricRegistry metricRegistry = new MetricRegistry();
	private static Sigar sigar;

	static {
		try {
			sigar = OsPlugin.newSigar();
		} catch (Exception e) {
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
	public void testCpuUtilisation() throws Exception {
		double cpu = getDoubleGauge("os.cpu.usage.sys") +
				getDoubleGauge("os.cpu.usage.user") +
				getDoubleGauge("os.cpu.usage.idle") +
				getDoubleGauge("os.cpu.usage.nice") +
				getDoubleGauge("os.cpu.usage.wait") +
				getDoubleGauge("os.cpu.usage.interrupt") +
				getDoubleGauge("os.cpu.usage.soft-interrupt") +
				getDoubleGauge("os.cpu.usage.stolen");

		assertEquals(1.0, cpu, 0.000001);

		assertTrue(getDoubleGauge("os.cpu.usage-percent") >= 0);
		assertTrue(getDoubleGauge("os.cpu.usage-percent") <= 1);
	}

	@Test
	public void testCpuInfo() throws Exception {
		assertTrue(getIntGauge("os.cpu.info.mhz") > 0);
		assertTrue(getIntGauge("os.cpu.info.cores") > 0);
	}

	@Test
	public void testMemoryUsage() throws Exception {
		assertEquals(getLongGauge("os.mem.usage.total"),
				getLongGauge("os.mem.usage.used") + getLongGauge("os.mem.usage.free"));

		final double usage = getDoubleGauge("os.mem.usage-percent");
		assertTrue(Double.toString(usage), usage >= 0);
		assertTrue(Double.toString(usage), usage <= 1);
	}

	@Test
	public void testSwapUsage() throws Exception {
		assertEquals(getLongGauge("os.swap.usage.total"),
				getLongGauge("os.swap.usage.used") + getLongGauge("os.swap.usage.free"));

		assertTrue(getDoubleGauge("os.swap.usage-percent") >= 0);
		assertTrue(getDoubleGauge("os.swap.usage-percent") <= 1);
		assertTrue(getLongGauge("os.swap.page.in") >= 0);
		assertTrue(getLongGauge("os.swap.page.out") >= 0);
	}

	@Test
	public void testGetConfigurationValid() throws Exception {
		assertEquals("bar", OsPlugin.getConfiguration(new String[]{"foo=bar"}).getValue("foo"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetConfigurationInvalid() throws Exception {
		OsPlugin.getConfiguration(new String[]{"foo"});
	}

	@Test
	public void testGetMeasurementSession() {
		final MeasurementSession measurementSession = OsPlugin.getMeasurementSession();
		assertEquals("os", measurementSession.getApplicationName());
		assertEquals("host", measurementSession.getInstanceName());
		assertNotNull(measurementSession.getHostName());
	}

	@Test
	@ExcludeOnTravis
	public void testNetworkMetrics() throws Exception {
		String baseName = name("os.net", GraphiteSanitizer.sanitizeGraphiteMetricSegment(sigar.getNetRouteList()[0].getIfname()));

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
	@ExcludeOnTravis
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
	@ExcludeOnTravis
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

	private int getIntGauge(String gaugeName) {
		return (Integer) getGauge(gaugeName);
	}

	private long getLongGauge(String gaugeName) {
		return (Long) getGauge(gaugeName);
	}

	private Object getGauge(String gaugeName) {
		return metricRegistry.getGauges().get(gaugeName).getValue();
	}
}
