package org.stagemonitor.web.session;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.MetricName;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class SessionCounter implements HttpSessionListener {

	private static final MetricName METRIC_NAME = name("http_sessions").build();

	@Override
	public void sessionCreated(HttpSessionEvent se) {
		Stagemonitor.getMetric2Registry().counter(METRIC_NAME).inc();
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		Stagemonitor.getMetric2Registry().counter(METRIC_NAME).dec();
	}
}
