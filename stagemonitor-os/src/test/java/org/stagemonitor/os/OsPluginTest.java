package org.stagemonitor.os;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.health.HealthCheckRegistry;

import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.junit.ConditionalTravisTestRunner;
import org.stagemonitor.junit.ExcludeOnTravis;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

@RunWith(ConditionalTravisTestRunner.class)
public class OsPluginTest {

	private Metric2Registry metricRegistry;
	private Sigar sigar;
	private static OsPlugin osPlugin;

	@BeforeClass
	public static void init() throws Exception {
		osPlugin = new OsPlugin();
	}

	@Before
	public void setUp() throws Exception {
		metricRegistry = new Metric2Registry();
		final ConfigurationRegistry configuration = mock(ConfigurationRegistry.class);
		final CorePlugin corePlugin = mock(CorePlugin.class);
		when(corePlugin.getElasticsearchClient()).thenReturn(mock(ElasticsearchClient.class));
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		osPlugin.initializePlugin(new StagemonitorPlugin.InitArguments(metricRegistry, configuration, mock(MeasurementSession.class), mock(HealthCheckRegistry.class)));
		this.sigar = osPlugin.getSigar();
	}

	@Test
	@Ignore
	public void testCpuUtilisation() throws Exception {
		double cpu = Double.NaN;
		for (int i = 0; i < 5 && Double.isNaN(cpu); i++) {
			setUp();
			cpu = getDoubleGauge(name("cpu_usage").type("sys").build()) +
					getDoubleGauge(name("cpu_usage").type("user").build()) +
					getDoubleGauge(name("cpu_usage").type("idle").build()) +
					getDoubleGauge(name("cpu_usage").type("nice").build()) +
					getDoubleGauge(name("cpu_usage").type("wait").build()) +
					getDoubleGauge(name("cpu_usage").type("interrupt").build()) +
					getDoubleGauge(name("cpu_usage").type("soft-interrupt").build()) +
					getDoubleGauge(name("cpu_usage").type("stolen").build());
		}

		assertEquals(100.0, cpu, 0.000001);

		assertTrue(getDoubleGauge(name("cpu_usage_percent").build()) >= 0);
		assertTrue(getDoubleGauge(name("cpu_usage_percent").build()) <= 100);
	}

	@Test
	public void testCpuInfo() throws Exception {
		assertTrue(getIntGauge(name("cpu_info_mhz").build()) > 0);
		assertTrue(getIntGauge(name("cpu_info_cores").build()) > 0);
	}

	@Test
	public void testMemoryUsage() throws Exception {
		assertEquals(getLongGauge(name("mem_usage").type("total").build()),
				getLongGauge(name("mem_usage").type("used").build()) + getLongGauge(name("mem_usage").type("free").build()));

		final double usage = getDoubleGauge(name("mem_usage_percent").build());
		assertTrue(Double.toString(usage), usage >= 0);
		assertTrue(Double.toString(usage), usage <= 100);
	}

	@Test
	public void testSwapUsage() throws Exception {
		assertEquals(getLongGauge(name("swap_usage").type("total").build()),
				getLongGauge(name("swap_usage").type("used").build()) + getLongGauge(name("swap_usage").type("free").build()));

		double swapPercent = getDoubleGauge(name("swap_usage_percent").build());
		assertTrue(swapPercent >= 0 || Double.isNaN(swapPercent));
		assertTrue(swapPercent <= 100 || Double.isNaN(swapPercent));
		assertTrue(getLongGauge(name("swap_pages").type("in").build()) >= 0);
		assertTrue(getLongGauge(name("swap_pages").type("out").build()) >= 0);
	}

	@Test
	public void testGetConfigurationValid() throws Exception {
		assertEquals("bar", OsConfigurationSourceInitializer.getConfiguration(new String[]{"foo=bar"}).getValue("foo"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetConfigurationInvalid() throws Exception {
		OsConfigurationSourceInitializer.getConfiguration(new String[]{"foo"});
	}

	@Test
	@ExcludeOnTravis
	public void testNetworkMetrics() throws Exception {
		final String ifname = sigar.getNetRouteList()[0].getIfname();

		assertTrue(getLongGauge(name("network_io").tag("ifname", ifname).type("read").unit("bytes").build()) >= 0);
		assertTrue(getLongGauge(name("network_io").tag("ifname", ifname).type("read").unit("packets").build()) >= 0);
		assertTrue(getLongGauge(name("network_io").tag("ifname", ifname).type("read").unit("errors").build()) >= 0);
		assertTrue(getLongGauge(name("network_io").tag("ifname", ifname).type("read").unit("dropped").build()) >= 0);

		assertTrue(getLongGauge(name("network_io").tag("ifname", ifname).type("write").unit("bytes").build()) >= 0);
		assertTrue(getLongGauge(name("network_io").tag("ifname", ifname).type("write").unit("packets").build()) >= 0);
		assertTrue(getLongGauge(name("network_io").tag("ifname", ifname).type("write").unit("errors").build()) >= 0);
		assertTrue(getLongGauge(name("network_io").tag("ifname", ifname).type("write").unit("dropped").build()) >= -1);
	}

	@Ignore("Fails on High Sierra")
	@Test
	@ExcludeOnTravis
	public void testFileSystemUsage() throws Exception {
		String mountPoint = getFirstMountPoint();
		assertEquals(getLongGauge(name("disk_usage").tag("mountpoint", mountPoint).type("total").build()),
				getLongGauge(name("disk_usage").tag("mountpoint", mountPoint).type("free").build()) +
						getLongGauge(name("disk_usage").tag("mountpoint", mountPoint).type("used").build()));
	}

	private String getFirstMountPoint() throws SigarException {
		@SuppressWarnings("unchecked")
		final Set<Map.Entry<String, FileSystem>> entries = (Set<Map.Entry<String, FileSystem>>) sigar.getFileSystemMap().entrySet();
		for (Map.Entry<String, FileSystem> e : entries) {
			if (e.getValue().getType() == FileSystem.TYPE_LOCAL_DISK) {
				return e.getKey();
			}
		}
		throw new IllegalStateException("No mount point found");
	}

	@Ignore("Fails on High Sierra")
	@Test
	@ExcludeOnTravis
	public void testFileSystemMetrics() throws Exception {
		String mountPoint = getFirstMountPoint();
		assertTrue(metricRegistry.getGauges().keySet().toString(), getDoubleGauge(name("disk_usage_percent").tag("mountpoint", mountPoint).build()) >= 0);
		assertTrue(getDoubleGauge(name("disk_usage_percent").tag("mountpoint", mountPoint).build()) <= 100);
		assertTrue(getLongGauge(name("disk_io").tag("mountpoint", mountPoint).type("read").build()) >= 0);
		assertTrue(getLongGauge(name("disk_io").tag("mountpoint", mountPoint).type("write").build()) >= 0);
	}

	private double getDoubleGauge(MetricName gaugeName) {
		return (Double) getGauge(gaugeName);
	}

	private int getIntGauge(MetricName gaugeName) {
		return (Integer) getGauge(gaugeName);
	}

	private long getLongGauge(MetricName gaugeName) {
		return (Long) getGauge(gaugeName);
	}

	private Object getGauge(MetricName gaugeName) {
		final Gauge gauge = metricRegistry.getGauges().get(gaugeName);
		assertNotNull(gaugeName + " not found in: " + metricRegistry.getGauges(), gauge);
		return gauge.getValue();
	}
}
