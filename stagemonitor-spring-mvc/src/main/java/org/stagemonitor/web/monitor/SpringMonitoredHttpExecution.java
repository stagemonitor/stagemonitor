package org.stagemonitor.web.monitor;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class SpringMonitoredHttpExecution extends MonitoredHttpExecution {

	private final List<HandlerMapping> allHandlerMappings;

	public SpringMonitoredHttpExecution(HttpServletRequest httpServletRequest,
										StatusExposingByteCountingServletResponse statusExposingResponse,
										FilterChain filterChain, Configuration configuration,
										List<HandlerMapping> allHandlerMappings) {

		super(httpServletRequest, statusExposingResponse, filterChain, configuration);
		this.allHandlerMappings = allHandlerMappings;
	}

	@Override
	public String getRequestName() {
		String name = null;
		for (HandlerMapping handlerMapping : allHandlerMappings) {
			try {
				HandlerExecutionChain handler = handlerMapping.getHandler(httpServletRequest);
				if (handler != null) {
					if (handler.getHandler() instanceof HandlerMethod) {
						HandlerMethod handlerMethod = (HandlerMethod) handler.getHandler();
						name = handlerMethod.getMethod().getName();
						return splitCamelCase(capitalize(name));
					}
				}
			} catch (Exception e) {
				// ignore, try next
			}
		}
		if (!configuration.isMonitorOnlySpringMvcRequests()) {
			name = super.getRequestName();
		}
		return name;
	}

	private static String capitalize(String self) {
		return Character.toUpperCase(self.charAt(0)) + self.substring(1);
	}

	private static String splitCamelCase(String s) {
		return s.replaceAll("(?<=[A-Z])(?=[A-Z][a-z])|(?<=[^A-Z])(?=[A-Z])|(?<=[A-Za-z])(?=[^A-Za-z])", " ");
	}
}
