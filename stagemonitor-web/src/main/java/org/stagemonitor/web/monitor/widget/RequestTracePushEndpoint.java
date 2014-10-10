package org.stagemonitor.web.monitor.widget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.requestmonitor.RequestTraceReporter;
import org.stagemonitor.web.monitor.HttpRequestTrace;

import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.HandshakeResponse;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ServerEndpoint(value = "/stagemonitor/request-trace", configurator = RequestTracePushEndpoint.class)
public class RequestTracePushEndpoint extends ServerEndpointConfig.Configurator implements RequestTraceReporter {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final ConcurrentMap<String, Session> httpSessionWebsocketSessionMap = new ConcurrentHashMap<String, Session>();

	@Override
	public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
		Object httpSession = request.getHttpSession();
		if (httpSession != null && httpSession instanceof HttpSession) {
			config.getUserProperties().put(HttpSession.class.getName(), ((HttpSession) httpSession).getId());
		}
	}

	@OnOpen
	public void onOpen(Session session) {
		logger.info("Connected ... " + session.getId());
		HttpSession httpSession = (HttpSession) session.getUserProperties().get(HttpSession.class.getName());
		if (httpSession != null) {
			httpSessionWebsocketSessionMap.put(httpSession.getId(), session);
		}
	}

	@OnMessage
	public void onMessage(String message, Session session) {
		logger.info("Received TEXT message: " + message);
	}

	@OnClose
	public void onClose(Session session, CloseReason closeReason) {
		logger.info(String.format("Session %s closed because of %s", session.getId(), closeReason));
		removeSession(session);
	}

	@OnError
	public void error(Session session, Throwable t) {
		logger.warn("Websocket connection error.");
		logger.warn(t.getMessage(), t);
		removeSession(session);
	}

	private void removeSession(Session session) {
		final String httpSessionId = (String) session.getUserProperties().get(HttpSession.class.getName());
		httpSessionWebsocketSessionMap.remove(httpSessionId);
	}

	@Override
	public <T extends RequestTrace> void reportRequestTrace(T requestTrace) {
		if (requestTrace instanceof HttpRequestTrace) {
			final Session session = httpSessionWebsocketSessionMap.get(((HttpRequestTrace) requestTrace).getSessionId());
			if (session.isOpen()) {
				try {
					session.getBasicRemote().sendText(JsonUtils.toJson(requestTrace));
				} catch (IOException e) {
					logger.warn(e.getMessage(), e);
				}
			} else {
				removeSession(session);
			}
		}
	}

	@Override
	public boolean isActive() {
		return true;  // TODO config key
	}
}
