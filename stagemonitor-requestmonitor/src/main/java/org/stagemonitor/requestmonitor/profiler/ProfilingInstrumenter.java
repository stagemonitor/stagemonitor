package org.stagemonitor.requestmonitor.profiler;

import java.lang.reflect.Modifier;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorJavassistInstrumenter;
import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

public class ProfilingInstrumenter extends StagemonitorJavassistInstrumenter {

	private RequestMonitorPlugin requestMonitorPlugin = Stagemonitor.getConfiguration(RequestMonitorPlugin.class);

	private String profilerPackage = Profiler.class.getPackage().getName();

	@Override
	public boolean isIncluded(String className) {
		if (!requestMonitorPlugin.isProfilerActive()) {
			return false;
		}
		return super.isIncluded(className) || isServlet(className);
	}

	private boolean isServlet(String className) {
		return className.endsWith("Servlet") && !className.contains("stagemonitor");
	}

	@Override
	public void transformClass(CtClass ctClass, ClassLoader loader) throws Exception {
		if (ctClass.getPackageName().equals(profilerPackage) || ctClass.isInterface() || !ClassUtils.canLoadClass(loader, "org.stagemonitor.requestmonitor.profiler.Profiler")) {
			return;
		}
		CtMethod[] declaredMethods = ctClass.getDeclaredMethods();
		for (CtMethod m : declaredMethods) {
			if (!Modifier.isNative(m.getModifiers())
					&& !Modifier.isAbstract(m.getModifiers())
					&& !Modifier.isFinal(m.getModifiers())
					&& ctClass.equals(m.getDeclaringClass())
					&& !m.getName().contains("access$")) {
				try {
					m.insertBefore("org.stagemonitor.requestmonitor.profiler.Profiler.start(\"" + getSignature(ctClass, m) + "\");");
					m.insertAfter("org.stagemonitor.requestmonitor.profiler.Profiler.stop();", true);
				} catch (CannotCompileException e) {
					// ignore
				} catch (NotFoundException e) {
					// ignore
				}
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
