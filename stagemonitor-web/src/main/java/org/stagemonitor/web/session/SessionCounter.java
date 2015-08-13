package org.stagemonitor.web.session;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.stagemonitor.core.Stagemonitor;

public class SessionCounter implements HttpSessionListener {

	@Override
	public void sessionCreated(HttpSessionEvent se) {
		Stagemonitor.getMetric2Registry().counter(name("http_sessions").build()).inc();
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		Stagemonitor.getMetric2Registry().counter(name("http_sessions").build()).dec();
	}
}
