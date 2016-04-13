package org.stagemonitor.logging;

import static net.bytebuddy.matcher.ElementMatchers.isPrivate;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;

/**
 * Tracks the rate of calls to a logger.
 * <p.>
 * Currently has support for Logback, slf4j's simple logger and JDK14LoggerAdapter, log4j 1.x and 2.x
 */
public class MeterLoggingTransformer extends StagemonitorByteBuddyTransformer {

	private final static ConcurrentMap<String, MetricName> loggingMetricNameCache = new ConcurrentHashMap<String, MetricName>();

	private final static Metric2Registry registry = Stagemonitor.getMetric2Registry();

	@Override
	protected ElementMatcher<? super TypeDescription> getExtraIncludeTypeMatcher() {
		return named("ch.qos.logback.classic.Logger")
				.or(named("org.slf4j.impl.SimpleLogger"))
				.or(named("org.apache.logging.log4j.spi.AbstractLogger"))
				.or(named("org.apache.log4j.Logger"))
				.or(named("org.slf4j.impl.JDK14LoggerAdapter"));
	}

	@Override
	protected ElementMatcher.Junction<? super MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return not(isPrivate())
				.and(named("trace")
						.or(named("debug"))
						.or(named("info"))
						.or(named("warn"))
						.or(named("error"))
						.or(named("fatal"))
				);
	}

	@Advice.OnMethodEnter
	private static void onEnterLog(@Advice.Origin("#m") String methodName) {
		trackLog(methodName);
	}

	public static void trackLog(String logLevel) {
		final Metric2Registry metric2Registry = Stagemonitor.getMetric2Registry();
		MetricName name = loggingMetricNameCache.get(logLevel);
		if (name == null) {
			name = name("logging").tag("log_level", logLevel).build();
			// we don't care about race conditions
			loggingMetricNameCache.put(logLevel, name);
		}
		registry.meter(name).mark();
	}

}
