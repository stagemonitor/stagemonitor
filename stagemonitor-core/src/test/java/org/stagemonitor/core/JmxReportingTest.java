package org.stagemonitor.core;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.management.ObjectInstance;

import com.codahale.metrics.Counter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.MBeanUtils;

public class JmxReportingTest {

	private Metric2Registry registry;

	@Before
	public void setUp() throws Exception {
		registry = new Metric2Registry();
		final Configuration configuration = Mockito.mock(Configuration.class);
		final CorePlugin corePlugin = mock(CorePlugin.class);
		when(corePlugin.reportToJMX()).thenReturn(true);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);

		new CorePlugin(mock(ElasticsearchClient.class)).registerReporters(registry, configuration, new MeasurementSession("JmxReportingTest", "test", "test"));
	}

	@Test
	public void testReportToJmx() throws Exception {
		final Counter counter = registry.counter(MetricName.name("test_counter").tag("foo", "bar").build());

		final ObjectInstance objectInstance = MBeanUtils.queryMBeans("metrics:name=test_counter.bar").iterator().next();

		Assert.assertNotNull(objectInstance);
		Assert.assertEquals(0L, MBeanUtils.getValueFromMBean(objectInstance, "Count"));

		counter.inc();
		Assert.assertEquals(1L, MBeanUtils.getValueFromMBean(objectInstance, "Count"));
	}
}
