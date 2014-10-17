package org.stagemonitor.web.monitor;

import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;

public interface MonitoredHttpRequestFactory {

	MonitoredHttpRequest createMonitoredHttpRequest(HttpServletRequest httpServletRequest, StatusExposingByteCountingServletResponse responseWrapper, FilterChain filterChain, Configuration configuration);
}
