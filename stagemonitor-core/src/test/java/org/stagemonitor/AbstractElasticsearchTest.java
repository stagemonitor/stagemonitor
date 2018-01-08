package org.stagemonitor;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.Netty4Plugin;
import org.junit.After;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.HttpClient;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractElasticsearchTest {

	private static final Logger logger = LoggerFactory.getLogger(AbstractElasticsearchTest.class);
	protected static Node node;
	protected static Client client;
	protected static AdminClient adminClient;
	protected static int elasticsearchPort;
	protected static ElasticsearchClient elasticsearchClient;
	protected static URL elasticsearchUrl;
	protected static CorePlugin corePlugin;

	@BeforeClass
	public static void beforeClass() throws Exception {
		Stagemonitor.init();
		if (node == null) {
			final File esHome = new File("build/elasticsearch");
			FileUtils.deleteQuietly(esHome);
			elasticsearchPort = getAvailablePort();
			logger.info("Elasticsearch port: {}", elasticsearchPort);
			final Settings settings = Settings.builder()
					.put("path.home", esHome.getAbsolutePath())
					.put("node.name", "junit-es-node")
					.put("http.port", elasticsearchPort)
					.put("path.logs", "build/elasticsearch/logs")
					.put("path.data", "build/elasticsearch/data")
					.put("transport.type", "local")
					.put("http.type", "netty4")
					.build();
			elasticsearchUrl = new URL("http://localhost:" + elasticsearchPort);
			AbstractElasticsearchTest.corePlugin = mock(CorePlugin.class);
			when(corePlugin.getElasticsearchUrl()).thenReturn(elasticsearchUrl);
			when(corePlugin.getElasticsearchUrls()).thenReturn(Collections.singletonList(elasticsearchUrl));
			when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
			elasticsearchClient = new ElasticsearchClient(corePlugin, new HttpClient(), -1, Collections.emptyList());
			when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient);

			node = new TestNode(settings, Collections.singletonList(Netty4Plugin.class));
			node.start();
			node.client().admin().cluster().prepareHealth().setWaitForGreenStatus().get();

			client = node.client();
			adminClient = client.admin();
			adminClient.cluster().prepareHealth()
					.setWaitForYellowStatus().execute().actionGet();
		}
		elasticsearchClient.checkEsAvailability();
		assertThat(elasticsearchClient.isElasticsearchAvailable()).isTrue();
	}

	private static class TestNode extends Node {
		public TestNode(Settings preparedSettings, Collection<Class<? extends Plugin>> classpathPlugins) {
			super(InternalSettingsPreparer.prepareEnvironment(preparedSettings, null), classpathPlugins);
		}
	}

	private static int getAvailablePort() throws IOException {
		final ServerSocket serverSocket = new ServerSocket(0);
		try {
			return serverSocket.getLocalPort();
		} finally {
			serverSocket.close();
		}
	}

	@After
	public final void after() {
		deleteAll();
	}

	protected void deleteAll() {
		node.client().admin().indices().prepareDelete("_all").get();
	}


	protected static void refresh() {
		node.client().admin().indices().prepareRefresh().get();
	}
}
