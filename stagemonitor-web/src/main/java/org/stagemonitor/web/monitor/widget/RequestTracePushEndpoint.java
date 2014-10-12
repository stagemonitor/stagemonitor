package org.stagemonitor.web.monitor.widget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.requestmonitor.RequestTraceReporter;
import org.stagemonitor.web.monitor.HttpRequestTrace;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ServerEndpoint(value = "/stagemonitor/request-trace/{connectionId}")
public class RequestTracePushEndpoint implements RequestTraceReporter {

	public static final String CONNECTION_ID = "x-stagemonitor-ws-connection-id";
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final ConcurrentMap<String, Session> connectionIdToWebsocketSessionMap = new ConcurrentHashMap<String, Session>();

	static RequestTracePushEndpoint instance;

	public RequestTracePushEndpoint() {
		RequestMonitor.addRequestTraceReporter(this);
		instance = this;
	}

	@OnOpen
	public void onOpen(@PathParam("connectionId") String connectionId, Session session) {
		connectionIdToWebsocketSessionMap.put(connectionId, session);
		session.getUserProperties().put("connectionId", connectionId);
	}

	@OnClose
	public void onClose(Session session, CloseReason closeReason) {
		removeSession(session);
	}

	@OnError
	public void error(Session session, Throwable t) {
		removeSession(session);
	}

	private void removeSession(Session session) {
		final String connectionId = (String) session.getUserProperties().get("connectionId");
		connectionIdToWebsocketSessionMap.remove(connectionId);
	}

	@Override
	public <T extends RequestTrace> void reportRequestTrace(T requestTrace) {
		if (requestTrace instanceof HttpRequestTrace) {
			final String connectionId = ((HttpRequestTrace) requestTrace).getWebsocketConnectionId();
			if (connectionId != null) {
				final Session session = connectionIdToWebsocketSessionMap.get(connectionId);
				if (session == null) {
					logger.warn("no session found for connection id {}", connectionId);
					return;
				}
				if (session.isOpen()) {
					pushRequestTrace(requestTrace, session);
				} else {
					removeSession(session);
				}
			}
		}
	}

	private <T extends RequestTrace> void pushRequestTrace(T requestTrace, Session session) {
		try {
			session.getBasicRemote().sendText(requestTrace.toJson());
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
	}

	@Override
	public boolean isActive() {
		return true;  // TODO config key
	}
}
