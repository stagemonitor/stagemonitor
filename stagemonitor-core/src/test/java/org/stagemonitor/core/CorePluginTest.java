package org.stagemonitor.core;

import org.junit.Assert;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.source.SimpleSource;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.ElasticsearchReporter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.HttpClient;

import javax.xml.bind.DatatypeConverter;
import java.io.Closeable;
import java.net.URL;
import java.net.URLDecoder;

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
	public void testCycleElasticsearchUrlsBasicAuth() throws Exception {
		CorePlugin corePlugin = ConfigurationRegistry.builder()
				.addOptionProvider(new CorePlugin())
				.addConfigSource(new SimpleSource("test")
						.add("stagemonitor.reporting.elasticsearch.url", "http://bla:1/,http://other:p%21p@bla:2,https://bla:3,http://bla:4/search")
						.add("stagemonitor.reporting.elasticsearch.username", "user")
						.add("stagemonitor.reporting.elasticsearch.password", "p@ssword!"))
				.build()
				.getConfig(CorePlugin.class);

		URL cycle1 = corePlugin.getElasticsearchUrl();

		String basicAuth = HttpClient.getBasicAuthFromUserInfo(cycle1);
		String testBasicAuth = "Basic " + DatatypeConverter.printBase64Binary("user:p@ssword!".getBytes());
		assertThat(basicAuth).isEqualTo(testBasicAuth);

		assertThat(cycle1.toString()).isEqualTo("http://user:p%40ssword%21@bla:1");

		URL cycle2 = corePlugin.getElasticsearchUrl();

		String basicAuthCycle2 = HttpClient.getBasicAuthFromUserInfo(cycle2);
		String testBasicAuthCycle2 = "Basic " + DatatypeConverter.printBase64Binary("other:p!p".getBytes());
		assertThat(basicAuth).isEqualTo(testBasicAuth);

		assertThat(cycle2.toString()).isEqualTo("http://other:p%21p@bla:2");

		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("https://user:p%40ssword%21@bla:3");
		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("http://user:p%40ssword%21@bla:4/search");
		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("http://user:p%40ssword%21@bla:1");
		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("http://other:p%21p@bla:2");
		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("https://user:p%40ssword%21@bla:3");
		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("http://user:p%40ssword%21@bla:4/search");
	}

	@Test
	public void testElasticsearchUrlsBasicAuthViaConfig() throws Exception {
		CorePlugin corePlugin = ConfigurationRegistry.builder()
				.addOptionProvider(new CorePlugin())
				.addConfigSource(new SimpleSource("test")
						.add("stagemonitor.reporting.elasticsearch.url", "http://bla:1")
						.add("stagemonitor.reporting.elasticsearch.username", "user")
						.add("stagemonitor.reporting.elasticsearch.password", "password"))
				.build()
				.getConfig(CorePlugin.class);

		assertThat(corePlugin.getElasticsearchUrl().toString()).isEqualTo("http://user:password@bla:1");
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

}
