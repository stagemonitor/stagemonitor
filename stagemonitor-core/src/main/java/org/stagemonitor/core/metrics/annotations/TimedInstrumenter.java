package org.stagemonitor.core.metrics.annotations;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.HashSet;
import java.util.Set;

import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.core.metrics.aspects.SignatureUtils;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.ClassUtils;

/**
 * Implementation for the {@link Timed} annotation
 */
public class TimedInstrumenter extends StagemonitorJavassistInstrumenter {

	private final Set<Class<?>> asyncCallAnnotations = new HashSet<Class<?>>();

	private static Metric2Registry metricRegistry;

	static {
		init();
	}

	public static void init() {
		metricRegistry = Stagemonitor.getMetric2Registry();
	}

	public TimedInstrumenter() {
		asyncCallAnnotations.add(ClassUtils.forNameOrNull("org.springframework.scheduling.annotation.Async"));
		asyncCallAnnotations.add(ClassUtils.forNameOrNull("org.springframework.scheduling.annotation.Scheduled"));
		asyncCallAnnotations.add(ClassUtils.forNameOrNull("org.springframework.scheduling.annotation.Schedules"));
		asyncCallAnnotations.add(ClassUtils.forNameOrNull("javax.ejb.Asynchronous"));
		asyncCallAnnotations.add(ClassUtils.forNameOrNull("javax.ejb.Schedule"));
		asyncCallAnnotations.add(ClassUtils.forNameOrNull("javax.ejb.Schedules"));
		asyncCallAnnotations.remove(null);
	}

	@Override
	public void transformClass(CtClass ctClass, ClassLoader loader) throws Exception {
		for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
			Timed timed = (Timed) ctMethod.getAnnotation(Timed.class);
			if (timed != null) {
				String signature = SignatureUtils.getSignature(ctMethod, timed.name(), timed.absolute());
				timeMethod(ctClass, ctMethod, signature);
			} else if (hasAsyncCallAnnotation(ctMethod)){
				String signature = SignatureUtils.getSignature(ctMethod, "", false);
				timeMethod(ctClass, ctMethod, signature);
			}
		}
	}

	private boolean hasAsyncCallAnnotation(CtMethod ctMethod) {
		for (Class<?> asyncCallAnnotation : asyncCallAnnotations) {
			if (ctMethod.hasAnnotation(asyncCallAnnotation)) {
				return true;
			}
		}
		return false;
	}

	private void timeMethod(CtClass ctClass, CtMethod ctMethod, String signature) throws CannotCompileException, NotFoundException {
		ctMethod.addLocalVariable("$_stm_time", ctClass.getClassPool().get(Timer.Context.class.getName()));
		ctMethod.insertBefore("$_stm_time = org.stagemonitor.core.metrics.annotations.TimedInstrumenter" +
				".time(\"" + signature + "\");");
		ctMethod.insertAfter("$_stm_time.stop();");
	}

	public static Timer.Context time(String signature) {
		return metricRegistry.timer(name("timer").tag("signature", signature).build()).time();
	}

}
