package org.stagemonitor.web.monitor;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;

import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;
import org.stagemonitor.web.monitor.spring.SpringMonitoredHttpRequest;

public class DefaultMonitoredHttpRequestFactory implements MonitoredHttpRequestFactory {

	private boolean springMvc;

	public DefaultMonitoredHttpRequestFactory() {
		try {
			Class.forName("org.springframework.web.servlet.HandlerMapping");
			springMvc = true;
		} catch (ClassNotFoundException e) {
			springMvc = false;
		}
	}

	@Override
	public MonitoredHttpRequest createMonitoredHttpRequest(HttpServletRequest httpServletRequest,
														   StatusExposingByteCountingServletResponse responseWrapper,
														   FilterChain filterChain, Configuration configuration) {
		if (springMvc) {
			return new SpringMonitoredHttpRequest(httpServletRequest, responseWrapper, filterChain, configuration);
		}
		return new MonitoredHttpRequest(httpServletRequest, responseWrapper, filterChain, configuration);
	}
}
