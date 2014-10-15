package org.stagemonitor.web.session;

import org.stagemonitor.core.StageMonitor;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@WebListener
public class SessionCounter implements HttpSessionListener {

	@Override
	public void sessionCreated(HttpSessionEvent se) {
		StageMonitor.getMetricRegistry().counter("web.sessions").inc();
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		StageMonitor.getMetricRegistry().counter("web.sessions").dec();
	}
}
