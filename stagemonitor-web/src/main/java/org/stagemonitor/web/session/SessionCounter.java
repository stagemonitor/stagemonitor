package org.stagemonitor.web.session;

import org.stagemonitor.core.StageMonitor;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

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
