package org.stagemonitor.web.servlet;

import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.web.servlet.filter.StatusExposingByteCountingServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;

public class DefaultMonitoredHttpRequestFactory implements MonitoredHttpRequestFactory {

	public DefaultMonitoredHttpRequestFactory() {
	}

	@Override
	public MonitoredHttpRequest createMonitoredHttpRequest(HttpServletRequest httpServletRequest,
														   StatusExposingByteCountingServletResponse responseWrapper,
														   FilterChain filterChain, ConfigurationRegistry configuration) {
		return new MonitoredHttpRequest(httpServletRequest, responseWrapper, filterChain, configuration);
	}
}
