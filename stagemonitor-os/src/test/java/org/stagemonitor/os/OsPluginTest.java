package org.stagemonitor.os;

import com.codahale.metrics.MetricRegistry;
import org.hyperic.sigar.Sigar;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.configuration.Configuration;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OsPluginTest {

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
