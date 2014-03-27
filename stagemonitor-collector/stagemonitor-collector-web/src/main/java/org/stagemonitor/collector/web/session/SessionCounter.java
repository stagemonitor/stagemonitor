package org.stagemonitor.collector.web.session;

import org.stagemonitor.collector.core.StageMonitorApplicationContext;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class SessionCounter implements HttpSessionListener {


	@Override
	public void sessionCreated(HttpSessionEvent se) {
		StageMonitorApplicationContext.getMetricRegistry().counter("web.sessions").inc();
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		StageMonitorApplicationContext.getMetricRegistry().counter("web.sessions").dec();
	}
}
