package org.stagemonitor.web.monitor;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.StageMonitor;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.springmvc.SpringMvcPlugin;

import javax.servlet.http.HttpServletRequest;

@Aspect
public class SpringMVCRequestNameDeterminerAspect {

	private final Configuration configuration;

	public SpringMVCRequestNameDeterminerAspect() {
		this(StageMonitor.getConfiguration());
	}

	public SpringMVCRequestNameDeterminerAspect(Configuration configuration) {
		this.configuration = configuration;
	}

	@Pointcut("execution(* org.springframework.web.servlet.DispatcherServlet+.getHandler(javax.servlet.http.HttpServletRequest))")
	private void getHandler() {
	}

	@Pointcut("if()")
	public static boolean requestTraceAvailable() {
		return RequestMonitor.getRequest() != null;
	}

	@AfterReturning(value = "getHandler() && requestTraceAvailable() && args(request)", returning = "handler", argNames = "request,handler")
	public void aroundGetHandler(HttpServletRequest request, HandlerExecutionChain handler) {
		// requests with empty names don't get monitored
		// requestNames with non null values don't get overwritten and avoid GetNameCallback#getName to be called
		String requestName = "";
		if (handler != null) {
			requestName = SpringMonitoredHttpRequest.getRequestNameFromHandler(handler);
		}
		if (requestName.isEmpty() && !configuration.getBoolean(SpringMvcPlugin.MONITOR_ONLY_SPRING_MVC_REQUESTS)) {
			requestName = MonitoredHttpRequest.getRequestNameByRequest(request , configuration);
		}
		RequestMonitor.getRequest().setName(requestName);
	}
}
