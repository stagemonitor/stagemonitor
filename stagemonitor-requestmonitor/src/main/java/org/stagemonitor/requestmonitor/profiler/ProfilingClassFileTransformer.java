package org.stagemonitor.requestmonitor.profiler;

import java.lang.reflect.Modifier;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;

public class ProfilingClassFileTransformer extends StagemonitorJavassistInstrumenter {

	@Override
	public void transformIncludedClass(CtClass ctClass) throws Exception {
		CtMethod[] declaredMethods = ctClass.getDeclaredMethods();
		for (CtMethod m : declaredMethods) {
			if (!Modifier.isNative(m.getModifiers()) && !Modifier.isAbstract(m.getModifiers())) {
				String signature = getSignature(ctClass, m);
				m.insertBefore("org.stagemonitor.requestmonitor.profiler.Profiler.start(\"" + signature + "\");");
				m.insertAfter("org.stagemonitor.requestmonitor.profiler.Profiler.stop();", true);
			}
		}
	}

	private String getSignature(CtClass clazz, CtMethod method) throws NotFoundException {
		StringBuilder signature = new StringBuilder()
				.append(method.getReturnType().getSimpleName()).append(" ")
				.append(clazz.getName()).append(".").append(method.getName()).append('(');
		CtClass[] parameterTypes = method.getParameterTypes();
		for (int i = 0; i < parameterTypes.length; i++) {
			if (i > 0) {
				signature.append(", ");
			}
			CtClass ctClass = parameterTypes[i];
			signature.append(ctClass.getSimpleName());
		}
		signature.append(')');
		return signature.toString();
	}
}
