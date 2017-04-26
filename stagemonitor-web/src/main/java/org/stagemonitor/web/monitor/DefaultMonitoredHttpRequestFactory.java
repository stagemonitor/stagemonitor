package org.stagemonitor.web.monitor;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;

import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

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
