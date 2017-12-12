package org.stagemonitor.core;

import org.junit.Assert;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.source.SimpleSource;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.ElasticsearchReporter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

import java.io.Closeable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class CorePluginTest {

	@Test
	public void testCycleElasticsearchUrls() throws Exception {
		CorePlugin corePlugin = ConfigurationRegistry.builder()
				.addOptionProvider(new CorePlugin())
				.addConfigSource(new SimpleSource("test")
						.add("stagemonitor.reporting.elasticsearch.url", "http://bla:1/,http://bla:2,http://bla:3"))
				.build()
				.getConfig(CorePlugin.class);

		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("http://bla:1");
		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("http://bla:2");
		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("http://bla:3");
		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("http://bla:1");
		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("http://bla:2");
		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("http://bla:3");
	}

	@Test
	public void testCycleElasticsearchUrlsEmptyString() throws Exception {
		CorePlugin corePlugin = ConfigurationRegistry.builder()
				.addOptionProvider(new CorePlugin())
				.addConfigSource(new SimpleSource("test")
						.add("stagemonitor.reporting.elasticsearch.url", ""))
				.build()
				.getConfig(CorePlugin.class);

		assertThat(corePlugin.getElasticsearchUrls()).isEmpty();
	}

	@Test
	public void testElasticsearchUrlsBasicAuth() throws Exception {
		CorePlugin corePlugin = ConfigurationRegistry.builder()
				.addOptionProvider(new CorePlugin())
				.addConfigSource(new SimpleSource("test")
						.add("stagemonitor.reporting.elasticsearch.url", "http://user:password@bla:1"))
				.build()
				.getConfig(CorePlugin.class);

		assertThat(corePlugin.getElasticsearchUrlsWithoutAuthenticationInformation()).isEqualTo("http://user:XXX@bla:1");
	}

	@Test
	public void testNoElasticsearchUrl() throws Exception {
		CorePlugin corePlugin = ConfigurationRegistry.builder()
				.addOptionProvider(new CorePlugin())
				.addConfigSource(new SimpleSource("test"))
				.build()
				.getConfig(CorePlugin.class);

		assertNull(corePlugin.getElasticsearchUrl());
	}

	@Test
	public void testOnlyLogElasticsearchMetricReports() throws Exception {
		Metric2Registry registry = new Metric2Registry();
		CorePlugin corePlugin = new CorePlugin(mock(ElasticsearchClient.class));
		ConfigurationRegistry configuration = ConfigurationRegistry.builder()
				.addOptionProvider(corePlugin)
				.addConfigSource(new SimpleSource("test")
					.add("stagemonitor.reporting.elasticsearch.onlyLogElasticsearchMetricReports", "true"))
				.build();

		corePlugin.registerReporters(registry, configuration, new MeasurementSession("OnlyLogElasticsearchMetricReportsTest", "test", "test"));

		boolean found = false;
		for (Closeable c : corePlugin.getReporters()) {
			found |= (c instanceof ElasticsearchReporter);
		}
		Assert.assertTrue("No ElasticsearchReporter found", found);
	}
}
