package de.isys.jawap.collector.profiler.instrument;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public abstract class SpringPerformanceMonitorAspect extends PerformanceMonitorAspect {

	@Pointcut("execution(* javax.servlet.Filter+.doFilter(..))")
	public void doFilter() {
	}

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

	@Pointcut("doFilter() || servletService() || renderView() || (resolveView() && !cflowbelow(resolveView())) || projectPointcut()")
	public void methodsToProfile() {
	}

}