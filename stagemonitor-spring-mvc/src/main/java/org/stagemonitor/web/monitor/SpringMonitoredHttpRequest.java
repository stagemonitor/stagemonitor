package org.stagemonitor.web.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.springmvc.SpringMvcPlugin;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class SpringMonitoredHttpRequest extends MonitoredHttpRequest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final List<HandlerMapping> allHandlerMappings;
	private final SpringMvcPlugin mvcPlugin;

	public SpringMonitoredHttpRequest(HttpServletRequest httpServletRequest,
									  StatusExposingByteCountingServletResponse statusExposingResponse,
									  FilterChain filterChain, Configuration configuration,
									  List<HandlerMapping> allHandlerMappings) {

		super(httpServletRequest, statusExposingResponse, filterChain, configuration);
		mvcPlugin = configuration.getConfig(SpringMvcPlugin.class);
		this.allHandlerMappings = allHandlerMappings;
	}

	@Override
	public String getRequestName() {
		String name = "";
		for (HandlerMapping handlerMapping : allHandlerMappings) {
			try {
				HandlerExecutionChain handler = handlerMapping.getHandler(httpServletRequest);
				name = getRequestNameFromHandler(handler);
			} catch (Exception e) {
				// ignore, try next
				logger.warn(e.getMessage(), e);
			}

			if (!name.isEmpty()) {
				return name;
			}
		}
		if (!mvcPlugin.isMonitorOnlySpringMvcRequests()) {
			name = super.getRequestName();
		}
		return name;
	}

	public static String getRequestNameFromHandler(HandlerExecutionChain handler) {
		if (handler != null && handler.getHandler() instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler.getHandler();
			return StringUtils.splitCamelCase(StringUtils.capitalize(handlerMethod.getMethod().getName()));
		}
		return "";
	}

}
