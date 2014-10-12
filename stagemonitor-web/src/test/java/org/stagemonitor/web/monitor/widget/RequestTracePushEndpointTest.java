package org.stagemonitor.web.monitor.widget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.web.monitor.HttpRequestTrace;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RequestTracePushEndpointTest {

	private int port = 9090;
	private Server server;
	private Session session;
	private String connectionId;
	private RequestTracePushEndpoint requestTracePushEndpoint;
	private WebSocketClient webSocketClient;

	@Before
	public void before() throws Exception {
		setUpServer();
		setUpClient();
		requestTracePushEndpoint = RequestTracePushEndpoint.instance;

	}

	private void setUpServer() throws Exception {
		try {
			server = new Server();
			ServerConnector connector = new ServerConnector(server);
//			port = getAvailablePort();
			connector.setPort(port);
			server.addConnector(connector);

			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
			context.setContextPath("/");
			server.setHandler(context);

			// Initialize javax.websocket layer
			ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);
			// Add WebSocket endpoint to javax.websocket layer
			wscontainer.addEndpoint(RequestTracePushEndpoint.class);

			server.start();
			server.dump(System.err);
		} catch (Exception e) {
			server.stop();
			throw new RuntimeException(e);
		}
	}

	private int getAvailablePort() throws IOException {
		final ServerSocket serverSocket = new ServerSocket(0);
		final int availablePort = serverSocket.getLocalPort();
		serverSocket.close();
		return availablePort;
	}

	private void setUpClient() throws IOException, DeploymentException {
		WebSocketContainer container = ContainerProvider.getWebSocketContainer();
		connectionId = UUID.randomUUID().toString();
		final URI path = URI.create("ws://localhost:" + port + "/stagemonitor/request-trace/" + connectionId);
		webSocketClient = new WebSocketClient();
		session = container.connectToServer(webSocketClient, path);

	}

	@After
	public void tearDown() throws Exception {
		session.close();
		server.stop();
	}

	@Test
	public void testWebSocket() throws Exception {
		requestTracePushEndpoint.reportRequestTrace(new HttpRequestTrace(new RequestTrace.GetNameCallback() {
			@Override
			public String getName() {
				return "test";
			}
		}, "/test", Collections.<String, String>emptyMap(), "GET", null, connectionId));

		Thread.sleep(100);

		assertNotNull(webSocketClient.message);
		final JsonNode httpRequestTrace = new ObjectMapper().readTree(webSocketClient.message);
		assertEquals("test", httpRequestTrace.get("name").asText());
		assertEquals("/test", httpRequestTrace.get("url").asText());
		assertEquals("GET", httpRequestTrace.get("method").asText());
	}

	@ClientEndpoint
	public static class WebSocketClient {
		String message;

		@OnMessage
		public void onWebSocketText(String message) {
			System.out.println("Received TEXT message: " + message);
			this.message = message;
		}

	}
}
