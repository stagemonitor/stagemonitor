package org.stagemonitor.core.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import org.junit.*;
import org.stagemonitor.core.Stagemonitor;

public class MBeanUtilsTest {

	private MetricRegistry metricRegistry = Stagemonitor.getMetricRegistry();

	@BeforeClass
	@AfterClass
	public static void resetStagemonitor() {
		Stagemonitor.reset();
		SharedMetricRegistries.clear();
	}

	@Test
	public void testRegisterMBean() throws Exception {
		MBeanUtils.registerMBean(MBeanUtils.queryMBean("java.lang:type=ClassLoading"), "LoadedClassCount", "jvm.classloading.count");
		final Gauge classLoadingGauge = metricRegistry.getGauges().get("jvm.classloading.count");
		assertNotNull(classLoadingGauge);
		assertTrue(((Integer) classLoadingGauge.getValue()) > 1);
	}
}
