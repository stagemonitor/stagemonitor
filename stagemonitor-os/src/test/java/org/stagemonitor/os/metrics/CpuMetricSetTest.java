package org.stagemonitor.os.metrics;

import com.codahale.metrics.MetricRegistry;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.junit.Test;
import org.stagemonitor.os.OsPlugin;

import java.io.IOException;

public class CpuMetricSetTest {

	private final MetricRegistry metricRegistry = new MetricRegistry();
	private final OsPlugin osPlugin = new OsPlugin();


	public CpuMetricSetTest() throws IOException, SigarException {
	}

	@Test
	public void testGetMetrics() throws Exception {
	}
}
