package org.stagemonitor.logging;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;

import static net.bytebuddy.matcher.ElementMatchers.isPrivate;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

/**
 * Tracks the rate of calls to a logger.
 * <p.>
 * Currently has support for Logback, slf4j's simple logger and JDK14LoggerAdapter, log4j 1.x and 2.x
 */
public class MeterLoggingTransformer extends StagemonitorByteBuddyTransformer {

	private final static MetricName.MetricNameTemplate logTemplate = name("logging").templateFor("log_level");

	private final static Metric2Registry registry = Stagemonitor.getMetric2Registry();

	@Override
	public ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
		return named("ch.qos.logback.classic.Logger")
				.or(named("org.slf4j.impl.SimpleLogger"))
				.or(named("org.apache.logging.log4j.spi.AbstractLogger"))
				.or(named("org.apache.log4j.Logger"))
				.or(named("org.slf4j.impl.JDK14LoggerAdapter"));
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription> getExtraMethodElementMatcher() {
		return not(isPrivate())
				.and(named("trace")
						.or(named("debug"))
						.or(named("info"))
						.or(named("warn"))
						.or(named("error"))
						.or(named("fatal"))
				);
	}

	@Advice.OnMethodEnter(inline = false)
	public static void onEnterLog(@Advice.Origin("#m") String methodName) {
		registry.meter(logTemplate.build(methodName)).mark();
	}

}
