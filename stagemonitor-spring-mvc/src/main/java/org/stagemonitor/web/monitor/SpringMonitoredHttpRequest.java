package org.stagemonitor.web.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.springmvc.SpringMvcPlugin;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Pattern;

public class SpringMonitoredHttpRequest extends MonitoredHttpRequest {

	public static final Pattern CAMEL_CASE = Pattern.compile("(?<=[A-Z])(?=[A-Z][a-z])|(?<=[^A-Z])(?=[A-Z])|(?<=[A-Za-z])(?=[^A-Za-z])");
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final List<HandlerMapping> allHandlerMappings;

	public SpringMonitoredHttpRequest(HttpServletRequest httpServletRequest,
									  StatusExposingByteCountingServletResponse statusExposingResponse,
									  FilterChain filterChain, Configuration configuration,
									  List<HandlerMapping> allHandlerMappings) {

		super(httpServletRequest, statusExposingResponse, filterChain, configuration);
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
		if (!configuration.getBoolean(SpringMvcPlugin.MONITOR_ONLY_SPRING_MVC_REQUESTS)) {
			name = super.getRequestName();
		}
		return name;
	}

	public static String getRequestNameFromHandler(HandlerExecutionChain handler) {
		if (handler != null && handler.getHandler() instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler.getHandler();
			return splitCamelCase(capitalize(handlerMethod.getMethod().getName()));
		}
		return "";
	}

	private static String capitalize(String self) {
		return Character.toUpperCase(self.charAt(0)) + self.substring(1);
	}

	private static String splitCamelCase(String s) {
		return CAMEL_CASE.matcher(s).replaceAll(" ");
	}
}
