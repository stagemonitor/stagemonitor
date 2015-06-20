package org.stagemonitor.core.configuration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.After;
import org.junit.BeforeClass;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class AbstractElasticsearchTest {

	protected static Node node;
	protected static Client client;
	protected static AdminClient adminClient;
	protected static int elasticsearchPort;
	protected static ElasticsearchClient elasticsearchClient;
	protected static String elasticsearchUrl;

	@BeforeClass
	public static void beforeClass() throws IOException {
		if (node == null) {
			FileUtils.deleteQuietly(new File("build/elasticsearch"));
			final NodeBuilder nodeBuilder = NodeBuilder.nodeBuilder().local(true);
			elasticsearchPort = getAvailablePort();
			nodeBuilder.settings()
					.put("name", "junit-es-node")
					.put("node.http.enabled", "false")
					.put("http.port", elasticsearchPort)
					.put("path.logs", "build/elasticsearch/logs")
					.put("path.data", "build/elasticsearch/data")
					.put("index.store.fs.memory.enabled", "true")
					.put("index.gateway.type", "none")
					.put("gateway.type", "none")
					.put("index.store.type", "memory")
					.put("index.number_of_shards", "1")
					.put("index.number_of_replicas", "0")
					.put("discovery.zen.ping.multicast.enabled", "false");
			elasticsearchUrl = "http://localhost:" + elasticsearchPort;
			final CorePlugin corePlugin = mock(CorePlugin.class);
			when(corePlugin.getElasticsearchUrl()).thenReturn(elasticsearchUrl);
			elasticsearchClient = new ElasticsearchClient(corePlugin);

			node = nodeBuilder.node();
			node.client().admin().cluster().prepareHealth().setWaitForGreenStatus().get();

			client = node.client();
			adminClient = client.admin();
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
	public void after() {
		node.client().admin().indices().prepareDelete("_all").get();
	}


	protected static void refresh() {
		node.client().admin().indices().prepareRefresh().get();
	}
}
