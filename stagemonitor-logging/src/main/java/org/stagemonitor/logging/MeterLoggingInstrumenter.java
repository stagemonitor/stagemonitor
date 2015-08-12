package org.stagemonitor.logging;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.HashSet;
import java.util.Set;

import javassist.CtClass;
import javassist.CtMethod;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

/**
 * Tracks the rate of calls to a logger.
 * <p/>
 * Currently has support for Logback, slf4j's simple logger and JDK14LoggerAdapter, log4j 1.x and 2.x
 */
public class MeterLoggingInstrumenter extends StagemonitorJavassistInstrumenter {

	private final static Metric2Registry registry = Stagemonitor.getMetric2Registry();

	private Set<String> methodsToInstrument = new HashSet<String>() {{
		add("trace");
		add("debug");
		add("info");
		add("warn");
		add("error");
		add("fatal");
	}};

	private static Set<String> loggerImplementations = new HashSet<String>() {{
		add("ch/qos/logback/classic/Logger");
		add("org/slf4j/impl/SimpleLogger");
		add("org/apache/logging/log4j/spi/AbstractLogger");
		add("org/apache/log4j/Logger");
		add("org/slf4j/impl/JDK14LoggerAdapter");
	}};

	@Override
	public boolean isIncluded(String className) {
		return loggerImplementations.contains(className);
	}

	@Override
	public void transformClass(CtClass ctClass, ClassLoader loader) throws Exception {
		for (CtMethod ctMethod : ctClass.getMethods()) {
			if (methodsToInstrument.contains(ctMethod.getName())) {
				ctMethod.insertBefore("org.stagemonitor.logging.MeterLoggingInstrumenter.trackLog(\"" + ctMethod.getName() + "\");");
			}
		}
	}

	public static void trackLog(String logLevel) {
		registry.meter(name("logging").tag("log_level", logLevel).build()).mark();
	}

}
