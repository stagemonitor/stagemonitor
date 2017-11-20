package org.stagemonitor.core;

import org.junit.Assert;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.source.ConfigurationSource;
import org.stagemonitor.configuration.source.SimpleSource;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.ElasticsearchReporter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

import java.io.Closeable;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class CorePluginTest {

	@Test
	public void testCycleElasticsearchUrls() throws Exception {
		CorePlugin corePlugin = new ConfigurationRegistry(
				Collections.singletonList(new CorePlugin()),
				Collections.<ConfigurationSource>singletonList(new SimpleSource("test")
						.add("stagemonitor.reporting.elasticsearch.url", "http://bla:1/,http://bla:2,http://bla:3")),
				null).getConfig(CorePlugin.class);

		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("http://bla:1");
		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("http://bla:2");
		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("http://bla:3");
		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("http://bla:1");
		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("http://bla:2");
		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("http://bla:3");
	}

	@Test
	public void testElasticsearchUrlsBasicAuth() throws Exception {
		CorePlugin corePlugin = new ConfigurationRegistry(
				Collections.singletonList(new CorePlugin()),
				Collections.singletonList(new SimpleSource("test")
						.add("stagemonitor.reporting.elasticsearch.url", "http://user:password@bla:1")),
				null).getConfig(CorePlugin.class);

		assertThat(corePlugin.getElasticsearchUrlsWithoutAuthenticationInformation()).isEqualTo("http://user:XXX@bla:1");
	}

	@Test
	public void testNoElasticsearchUrl() throws Exception {
		CorePlugin corePlugin = new ConfigurationRegistry(
				Collections.singletonList(new CorePlugin()),
				Collections.<ConfigurationSource>singletonList(new SimpleSource("test")),
				null).getConfig(CorePlugin.class);

		assertNull(corePlugin.getElasticsearchUrl());
	}

	@Test
	public void testOnlyLogElasticsearchMetricReports() throws Exception {
		Metric2Registry registry = new Metric2Registry();
		CorePlugin corePlugin = new CorePlugin(mock(ElasticsearchClient.class));
		ConfigurationRegistry configuration = new ConfigurationRegistry(
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
