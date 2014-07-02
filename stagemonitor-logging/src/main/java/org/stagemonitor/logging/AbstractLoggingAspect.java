package org.stagemonitor.logging;

import com.codahale.metrics.MetricRegistry;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.stagemonitor.core.StageMonitor;

@Aspect
public abstract class AbstractLoggingAspect {

	public static final boolean ACTIVE = !StageMonitor.getConfiguration().getDisabledPlugins()
			.contains(LoggingPlugin.class.getSimpleName()) && StageMonitor.getConfiguration().isStagemonitorActive();

	protected MetricRegistry registry = StageMonitor.getMetricRegistry();

	@Pointcut("execution(* org.slf4j.Logger+.trace(..)) || execution(* org.slf4j.Logger+.debug(..)) " +
			"|| execution(* org.slf4j.Logger+.info(..)) || execution(* org.slf4j.Logger+.warn(..)) " +
			"|| execution(* org.slf4j.Logger+.error(..))")
	public void slf4jLoggingMethods() {
	}

	@Pointcut("execution(* org.apache.commons.logging.Log+.trace(..)) || execution(* org.apache.commons.logging.Log+.debug(..)) " +
			"|| execution(* org.apache.commons.logging.Log+.info(..)) || execution(* org.apache.commons.logging.Log+.warn(..)) " +
			"|| execution(* org.apache.commons.logging.Log+.error(..)) || execution(* org.apache.commons.logging.Log+.fatal(..))")
	public void apacheCommonsLoggingMethods() {
	}

	@Pointcut("if()")
	public static boolean ifActivated() {
		return ACTIVE;
	}

	@Pointcut("ifActivated() && (slf4jLoggingMethods() || apacheCommonsLoggingMethods())")
	public void loggingPointcut() {
	}
}
