package org.stagemonitor.core.util;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractEmbeddedServerTest {
	protected Server server;

	@Before
	public final void setUp() throws Exception {
		server = new Server(0);
	}

	@After
	public final void tearDown() throws Exception {
		server.stop();
	}

	protected int getPort() {
		return ((NetworkConnector)server.getConnectors()[0]).getLocalPort();
	}

	protected void startWithHandler(Handler handler) throws Exception {
		server.setHandler(handler);
		server.start();
	}
}
