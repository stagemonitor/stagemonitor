package org.stagemonitor.logging;

import com.codahale.metrics.MetricRegistry;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;

@Aspect
public abstract class AbstractLoggingAspect {

	public static final boolean ACTIVE = !Stagemonitor.getConfiguration(CorePlugin.class).getDisabledPlugins()
			.contains(LoggingPlugin.class.getSimpleName()) && Stagemonitor.getConfiguration(CorePlugin.class).isStagemonitorActive();

	protected MetricRegistry registry = Stagemonitor.getMetricRegistry();

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
