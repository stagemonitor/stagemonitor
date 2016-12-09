package org.stagemonitor.core;

import java.io.Closeable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import org.junit.Assert;

import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.source.ConfigurationSource;
import org.stagemonitor.core.configuration.source.SimpleSource;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.ElasticsearchReporter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

public class CorePluginTest {

	@Test
	public void testCycleElasticsearchUrls() throws Exception {
		CorePlugin corePlugin = new Configuration(
				Collections.singletonList(new CorePlugin()),
				Collections.<ConfigurationSource>singletonList(new SimpleSource("test")
						.add("stagemonitor.elasticsearch.url", "http://bla:1/,http://bla:2,http://bla:3")),
				null).getConfig(CorePlugin.class);

		assertEquals("http://bla:1", corePlugin.getElasticsearchUrl());
		assertEquals("http://bla:2", corePlugin.getElasticsearchUrl());
		assertEquals("http://bla:3", corePlugin.getElasticsearchUrl());
		assertEquals("http://bla:1", corePlugin.getElasticsearchUrl());
		assertEquals("http://bla:2", corePlugin.getElasticsearchUrl());
		assertEquals("http://bla:3", corePlugin.getElasticsearchUrl());
	}

	@Test
	public void testNoElasticsearchUrl() throws Exception {
		CorePlugin corePlugin = new Configuration(
				Collections.singletonList(new CorePlugin()),
				Collections.<ConfigurationSource>singletonList(new SimpleSource("test")),
				null).getConfig(CorePlugin.class);

		assertNull(corePlugin.getElasticsearchUrl());
	}

	@Test
	public void testOnlyLogElasticsearchMetricReports() throws Exception {
		Metric2Registry registry = new Metric2Registry();
		CorePlugin corePlugin = new CorePlugin(mock(ElasticsearchClient.class));
		Configuration configuration = new Configuration(
				Collections.singletonList(corePlugin),
				Collections.<ConfigurationSource>singletonList(new SimpleSource("test")
					.add("stagemonitor.reporting.elasticsearch.onlyLogElasticsearchMetricReports", "true")),
				null);

		corePlugin.registerReporters(registry, configuration, new MeasurementSession("OnlyLogElasticsearchMetricReportsTest", "test", "test"));

		boolean found = false;
		for (Closeable c : corePlugin.getReporters()) {
			found |= (c instanceof ElasticsearchReporter);
		}
		Assert.assertTrue("No ElasticsearchReporter found", found);
	}
}
