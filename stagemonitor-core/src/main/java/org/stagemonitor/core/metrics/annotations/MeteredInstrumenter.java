package org.stagemonitor.core.metrics.annotations;

import com.codahale.metrics.annotation.Metered;
import javassist.CtClass;
import javassist.CtMethod;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.core.metrics.aspects.SignatureUtils;

/**
 * Implementation for the {@link Metered} annotation
 */
public class MeteredInstrumenter extends StagemonitorJavassistInstrumenter {

	@Override
	public void transformClass(CtClass ctClass, ClassLoader loader) throws Exception {
		for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
			Metered Metered = (Metered) ctMethod.getAnnotation(Metered.class);
			if (Metered != null) {
				final String signature = SignatureUtils.getSignature(ctMethod, Metered.name(),
						Metered.absolute());
				ctMethod.insertBefore("org.stagemonitor.core.Stagemonitor.getMetricRegistry()" +
						".meter(\"meter." + signature + "\").mark();");
			}
		}
	}
}
