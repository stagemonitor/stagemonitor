package de.isys.jawap.collector.web.monitor;

import de.isys.jawap.collector.core.Configuration;
import de.isys.jawap.collector.web.monitor.filter.StatusExposingServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;

public class SpringHttpRequestContextMonitoredExecution extends HttpRequestContextMonitoredExecution {


	private final List<RequestMappingHandlerMapping> allHandlerMappings;

	public SpringHttpRequestContextMonitoredExecution(HttpServletRequest httpServletRequest,
													  StatusExposingServletResponse statusExposingResponse,
													  FilterChain filterChain, Configuration configuration,
													  List<RequestMappingHandlerMapping> allHandlerMappings) {
		super(httpServletRequest, statusExposingResponse, filterChain, configuration);

		this.allHandlerMappings = allHandlerMappings;
	}

	@Override
	public String getRequestName() {
		String name = null;
		for (RequestMappingHandlerMapping handlerMapping : allHandlerMappings) {
			try {
				HandlerExecutionChain handler = handlerMapping.getHandler(httpServletRequest);
				if (handler.getHandler() instanceof HandlerMethod) {
					HandlerMethod handlerMethod = (HandlerMethod) handler.getHandler();
					name = handlerMethod.getMethod().getName();
					name = splitCamelCase(capitalize(name));
				} else {
					name = handler.getHandler().toString();
				}
				if (name != null && !name.isEmpty()) {
					break;
				}
			} catch (Exception e) {
				// ignore, try next
			}
		}
		if (name == null) {
			name = super.getRequestName();
		}
		return name;
	}

	private static String capitalize(String self) {
		if (self == null || self.length() == 0) return self;
		return Character.toUpperCase(self.charAt(0)) + self.substring(1);
	}

	private static String splitCamelCase(String s) {
		return s.replaceAll("(?<=[A-Z])(?=[A-Z][a-z])|(?<=[^A-Z])(?=[A-Z])|(?<=[A-Za-z])(?=[^A-Za-z])", " ");
	}
}
