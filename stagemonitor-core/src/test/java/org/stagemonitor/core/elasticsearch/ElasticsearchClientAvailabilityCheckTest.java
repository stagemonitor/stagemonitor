package org.stagemonitor.core.elasticsearch;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.util.HttpClient;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticsearchClientAvailabilityCheckTest {

	private ElasticsearchClient elasticsearchClient;

	@Before
	public void setUp() throws Exception {
		final CorePlugin corePlugin = mock(CorePlugin.class);
		when(corePlugin.getElasticsearchUrl()).thenReturn("http://localhost:41234");
		when(corePlugin.getElasticsearchUrls()).thenReturn(Collections.singletonList("http://localhost:41234"));
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(10000);
		elasticsearchClient = new ElasticsearchClient(corePlugin, new HttpClient(), 1);
	}

	@Test
	public void testNotAvailable() throws Exception {
		Thread.sleep(1100);
		assertFalse(elasticsearchClient.isElasticsearchAvailable());

		Server server = new Server(41234);
		server.setHandler(new AbstractHandler() {
			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				baseRequest.setHandled(true);
			}
		});
		server.start();

		Thread.sleep(1100);
		assertTrue(elasticsearchClient.isElasticsearchAvailable());
		server.stop();
	}

}
