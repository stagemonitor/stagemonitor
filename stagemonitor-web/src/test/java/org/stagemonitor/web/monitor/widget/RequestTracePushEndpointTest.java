package org.stagemonitor.web.monitor.widget;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerEndpoint;
import java.net.URI;

public class RequestTracePushEndpointTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestTracePushEndpointTest.class);

	private static final int PORT = 9090;
	private WebSocketContainer container;
	private Server server;

	@Before
	public void before() throws Exception {
		setUpServer();

		container = ContainerProvider.getWebSocketContainer();
	}

	private void setUpServer() throws Exception {
		try {
			server = new Server();
			ServerConnector connector = new ServerConnector(server);
			connector.setPort(PORT);
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

	@After
	public void tearDown() throws Exception {
		server.join();
	}

	@Test
	public void testWebSocket() throws Exception {
		try {
			final URI path = URI.create("ws://localhost:" + PORT + "/stagemonitor/request-trace");
			Session session = container.connectToServer(EventSocket.class, path);
			session.getBasicRemote().sendText("Hello");
			session.close();
		} finally {
			server.stop();
		}
	}

	@ClientEndpoint
	public static class EventSocket {
		@OnOpen
		public void onWebSocketConnect(Session sess) {
			System.out.println("Socket Connected: " + sess);
		}

		@OnMessage
		public void onWebSocketText(String message) {
			System.out.println("Received TEXT message: " + message);
		}

		@OnClose
		public void onWebSocketClose(CloseReason reason) {
			System.out.println("Socket Closed: " + reason);
		}

		@OnError
		public void onWebSocketError(Throwable cause) {
			cause.printStackTrace(System.err);
		}
	}
}
