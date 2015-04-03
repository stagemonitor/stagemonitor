package org.stagemonitor.core.metrics.annotations;

import com.codahale.metrics.annotation.ExceptionMetered;
import javassist.CtClass;
import javassist.CtMethod;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.core.metrics.aspects.SignatureUtils;

/**
 * Implementation for the {@link ExceptionMetered} annotation
 */
public class ExceptionMeteredInstrumenter extends StagemonitorJavassistInstrumenter {

	@Override
	public void transformIncludedClass(CtClass ctClass) throws Exception {
		for (CtMethod ctMethod : ctClass.getMethods()) {
			ExceptionMetered exceptionMetered = (ExceptionMetered) ctMethod.getAnnotation(ExceptionMetered.class);
			if (exceptionMetered != null) {
				CtClass exceptionType = ctClass.getClassPool().get(exceptionMetered.cause().getName());
				final String signature = SignatureUtils.getSignature(ctMethod, exceptionMetered.name(),
						exceptionMetered.absolute());
				String src = "{" +
						"	org.stagemonitor.core.Stagemonitor.getMetricRegistry()" +
						"			.meter(\"meter." + signature + ".exceptions\").mark();" +
						"	throw e;" +
						"}";
				ctMethod.addCatch(src, exceptionType, "e");
			}
		}
	}
}
