package org.stagemonitor.logging;

import java.util.HashSet;
import java.util.Set;

import javassist.CtClass;
import javassist.CtMethod;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;

/**
 * Tracks the rate of calls to a logger.
 * <p/>
 * Currently has support for Logback, slf4j's simple logger and JDK14LoggerAdapter, log4j 1.x and 2.x
 */
public class MeterLoggingInstrumenter extends StagemonitorJavassistInstrumenter {

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
				ctMethod.insertBefore("org.stagemonitor.core.Stagemonitor.getMetricRegistry()" +
						".meter(\"logging." + ctMethod.getName() + "\").mark();");
			}
		}
	}

}
