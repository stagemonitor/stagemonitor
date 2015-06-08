package org.stagemonitor.web.session;

import org.stagemonitor.core.Stagemonitor;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class SessionCounter implements HttpSessionListener {

	@Override
	public void sessionCreated(HttpSessionEvent se) {
		Stagemonitor.getMetricRegistry().counter("web.sessions").inc();
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		Stagemonitor.getMetricRegistry().counter("web.sessions").dec();
	}
}
