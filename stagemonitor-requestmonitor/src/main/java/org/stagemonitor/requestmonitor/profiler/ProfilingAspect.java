package org.stagemonitor.requestmonitor.profiler;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public abstract class ProfilingAspect {

	@Pointcut
	public abstract void methodsToProfile();

	@Pointcut("within(org.stagemonitor..*)")
	private void stagemonitorCollector() {
	}

	@Pointcut("!within(@org.aspectj.lang.annotation.Aspect *)")
	private void dontWeaveInOtherAspects() {
	}

	@Pointcut("execution(* javax.servlet.Servlet+.service(..)) || execution(* javax.servlet.http.HttpServlet+.do*(..))")
	public void servletService() {
	}

	@Pointcut("execution(* org.springframework.web.servlet.View+.render(..))")
	public void renderView() {
	}

	@Pointcut("execution(* org.springframework.web.servlet.ViewResolver+.resolveViewName(..))")
	public void resolveView() {
	}

	@Pointcut("servletService() || renderView() || (resolveView() && !cflowbelow(resolveView()))")
	public void webMethods() {
	}

	@Pointcut("(methodsToProfile() || webMethods()) && !stagemonitorCollector() && dontWeaveInOtherAspects()")
	private void applicationMethodsToProfile() {
	}

	@Pointcut("if()")
	public static boolean isProfilingActive() {
		return Profiler.isProfilingActive();
	}

	@Before("applicationMethodsToProfile()")
	public void startProfiling(JoinPoint.StaticPart joinPoint) {
		Profiler.start(joinPoint.getSignature().toString());
	}

	@After("applicationMethodsToProfile()")
	public void stopProfiling() {
		Profiler.stop();
	}
}