package org.stagemonitor.requestmonitor.profiler;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public abstract class SpringProfilingAspect extends ProfilingAspect {

	@Pointcut("execution(* javax.servlet.Servlet+.service(..))")
	public void servletService() {
	}

	@Pointcut("execution(* org.springframework.web.servlet.View+.render(..))")
	public void renderView() {
	}

	@Pointcut("execution(* org.springframework.web.servlet.ViewResolver+.resolveViewName(..))")
	public void resolveView() {
	}

	@Pointcut
	public abstract void projectPointcut();

	@Pointcut("servletService() || renderView() || (resolveView() && !cflowbelow(resolveView())) || projectPointcut()")
	public void methodsToProfile() {
	}

}