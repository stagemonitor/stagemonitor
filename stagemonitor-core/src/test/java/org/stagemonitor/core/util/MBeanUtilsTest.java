package org.stagemonitor.core.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import com.codahale.metrics.Gauge;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

public class MBeanUtilsTest {

	private Metric2Registry metricRegistry = Stagemonitor.getMetric2Registry();

	@BeforeClass
	@AfterClass
	public static void resetStagemonitor() {
		Stagemonitor.reset();
	}

	@Test
	public void testRegisterMBean() throws Exception {
		MBeanUtils.registerMBean(MBeanUtils.queryMBean("java.lang:type=ClassLoading"), "LoadedClassCount", name("jvm_class_count").build());
		final Gauge classLoadingGauge = metricRegistry.getGauges().get(name("jvm_class_count").build());
		assertNotNull(classLoadingGauge);
		assertTrue(((Integer) classLoadingGauge.getValue()) > 1);
	}
}
