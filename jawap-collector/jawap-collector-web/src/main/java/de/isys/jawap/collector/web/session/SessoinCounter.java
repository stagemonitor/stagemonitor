package de.isys.jawap.collector.web.session;

import de.isys.jawap.collector.core.JawapApplicationContext;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class SessoinCounter implements HttpSessionListener {


	@Override
	public void sessionCreated(HttpSessionEvent se) {
		JawapApplicationContext.getMetricRegistry().counter("web.sessions").inc();
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		JawapApplicationContext.getMetricRegistry().counter("web.sessions").dec();
	}
}
