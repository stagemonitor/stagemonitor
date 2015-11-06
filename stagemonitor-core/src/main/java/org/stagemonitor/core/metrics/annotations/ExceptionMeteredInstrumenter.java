package org.stagemonitor.core.metrics.annotations;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import com.codahale.metrics.annotation.ExceptionMetered;
import javassist.CtClass;
import javassist.CtMethod;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.core.metrics.aspects.SignatureUtils;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

/**
 * Implementation for the {@link ExceptionMetered} annotation
 */
public class ExceptionMeteredInstrumenter extends StagemonitorJavassistInstrumenter {

	private static Metric2Registry metricRegistry;

	static {
		init();
	}

	static void init() {
		metricRegistry = Stagemonitor.getMetric2Registry();
	}

	@Override
	public void transformClass(CtClass ctClass, ClassLoader loader) throws Exception {
		for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
			ExceptionMetered exceptionMetered = (ExceptionMetered) ctMethod.getAnnotation(ExceptionMetered.class);
			if (exceptionMetered != null) {
				CtClass exceptionType = ctClass.getClassPool().get(exceptionMetered.cause().getName());
				final String signature = SignatureUtils.getSignature(ctMethod, exceptionMetered.name(),
						exceptionMetered.absolute());
				String src = "{" +
						"	org.stagemonitor.core.metrics.annotations.ExceptionMeteredInstrumenter" +
						"		.meterException(\"" + signature + "\");" +
						"	throw e;" +
						"}";
				ctMethod.addCatch(src, exceptionType, "e");
			}
		}
	}

	public static void meterException(String signature) {
		metricRegistry.meter(name("exception_rate").tag("signature", signature).build()).mark();
	}

}
