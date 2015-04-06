package org.stagemonitor.core.metrics.annotations;

import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;
import javassist.CtClass;
import javassist.CtMethod;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.core.metrics.aspects.SignatureUtils;

/**
 * Implementation for the {@link Timed} annotation
 */
public class TimedInstrumenter extends StagemonitorJavassistInstrumenter {

	@Override
	public void transformIncludedClass(CtClass ctClass) throws Exception {
		for (CtMethod ctMethod : ctClass.getMethods()) {
			Timed timed = (Timed) ctMethod.getAnnotation(Timed.class);
			if (timed != null) {
				ctMethod.addLocalVariable("$_stm_time", ctClass.getClassPool().get(Timer.Context.class.getName()));
				String signature = SignatureUtils.getSignature(ctMethod, timed.name(), timed.absolute());
				ctMethod.insertBefore("$_stm_time = org.stagemonitor.core.Stagemonitor.getMetricRegistry()" +
						".timer(\"timer." + signature + "\").time();");
				ctMethod.insertAfter("$_stm_time.stop();");
			}
		}
	}

}
