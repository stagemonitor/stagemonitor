package org.stagemonitor.core.metrics.annotations;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.annotation.ExceptionMetered;
import javassist.CtClass;
import javassist.CtMethod;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.core.metrics.aspects.SignatureUtils;

public class ExceptionMeteredInstrumenter extends StagemonitorJavassistInstrumenter {

	@Override
	public void transformIncludedClass(CtClass ctClass) throws Exception {
		for (CtMethod ctMethod : ctClass.getMethods()) {
			ExceptionMetered exceptionMetered = (ExceptionMetered) ctMethod.getAnnotation(ExceptionMetered.class);
			if (exceptionMetered != null) {
				CtClass exceptionType = ctClass.getClassPool().get(exceptionMetered.cause().getName());
				final String signature = SignatureUtils.getSignature(ctMethod, exceptionMetered.name(),
						exceptionMetered.absolute());
				String src =
						"{" +
						"	org.stagemonitor.core.metrics.annotations.ExceptionMeteredInstrumenter.meterException(\"" + signature + "\");" +
						"	throw e;" +
						"}";
				ctMethod.addCatch(src, exceptionType, "e");
			}
		}
	}

	public static void meterException(String signature) {
		Stagemonitor.getMetricRegistry().meter(name("meter", signature, ExceptionMetered.DEFAULT_NAME_SUFFIX)).mark();
	}
}
