package org.stagemonitor.core.metrics.annotations;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import com.codahale.metrics.annotation.Metered;
import javassist.CtClass;
import javassist.CtMethod;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.core.metrics.aspects.SignatureUtils;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

/**
 * Implementation for the {@link Metered} annotation
 */
public class MeteredInstrumenter extends StagemonitorJavassistInstrumenter {

	private static Metric2Registry metricRegistry;

	static {
		init();
	}

	public static void init() {
		metricRegistry = Stagemonitor.getMetric2Registry();
	}

	@Override
	public void transformClass(CtClass ctClass, ClassLoader loader) throws Exception {
		for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
			Metered Metered = (Metered) ctMethod.getAnnotation(Metered.class);
			if (Metered != null) {
				final String signature = SignatureUtils.getSignature(ctMethod, Metered.name(),
						Metered.absolute());
				ctMethod.insertBefore("org.stagemonitor.core.metrics.annotations.MeteredInstrumenter" +
						".meter(\"" + signature + "\");");
			}
		}
	}

	public static void meter(String signature) {
		metricRegistry.meter(name("rate").tag("signature", signature).build()).mark();
	}

}
