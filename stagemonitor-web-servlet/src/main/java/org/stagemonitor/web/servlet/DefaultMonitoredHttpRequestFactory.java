package org.stagemonitor.web.servlet;

import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.util.ExecutorUtils;
import org.stagemonitor.web.servlet.filter.StatusExposingByteCountingServletResponse;

import java.util.concurrent.ExecutorService;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;

public class DefaultMonitoredHttpRequestFactory implements MonitoredHttpRequestFactory {

	private final ExecutorService executorService;

	public DefaultMonitoredHttpRequestFactory(CorePlugin corePlugin) {
		executorService = ExecutorUtils.createSingleThreadDeamonPool("user-agent-parser", 1000, corePlugin);
	}

	@Override
	public MonitoredHttpRequest createMonitoredHttpRequest(HttpServletRequest httpServletRequest,
														   StatusExposingByteCountingServletResponse responseWrapper,
														   FilterChain filterChain, ConfigurationRegistry configuration) {
		return new MonitoredHttpRequest(httpServletRequest, responseWrapper, filterChain, configuration, executorService);
	}
}
