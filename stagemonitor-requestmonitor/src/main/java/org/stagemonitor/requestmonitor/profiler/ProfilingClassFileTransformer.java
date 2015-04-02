package org.stagemonitor.requestmonitor.profiler;

import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import org.stagemonitor.agent.StagemonitorClassFileTransformer;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

public class ProfilingClassFileTransformer implements StagemonitorClassFileTransformer {

	public Collection<String> includes;

	public Collection<String> excludes;

	public Collection<String> excludeContaining;

	public ProfilingClassFileTransformer() {
		RequestMonitorPlugin requestMonitorPlugin = RequestMonitorPlugin.getSimpleInstance();

		excludeContaining = new ArrayList<String>(requestMonitorPlugin.getExcludeContaining().size());
		for (String exclude : requestMonitorPlugin.getExcludeContaining()) {
			excludeContaining.add(exclude.replace('.', '/'));
		}

		excludes = new ArrayList<String>(requestMonitorPlugin.getExcludePackages().size());
		for (String exclude : requestMonitorPlugin.getExcludePackages()) {
			excludes.add(exclude.replace('.', '/'));
		}

		includes = new ArrayList<String>(requestMonitorPlugin.getIncludePackages().size());
		for (String include : requestMonitorPlugin.getIncludePackages()) {
			includes.add(include.replace('.', '/'));
		}
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class classBeingRedefined,
							ProtectionDomain protectionDomain, byte[] classfileBuffer)
			throws IllegalClassFormatException {
		if (!isIncluded(className)) {
			return classfileBuffer;
		}

		try {
			ClassPool cp = ClassPool.getDefault();
			cp.insertClassPath(new LoaderClassPath(loader));
			CtClass cc = cp.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));

			try {
				CtMethod[] declaredMethods = cc.getDeclaredMethods();
				for (int i = 0; i < declaredMethods.length; i++) {
					CtMethod m = declaredMethods[i];
					if (!Modifier.isNative(m.getModifiers()) && !Modifier.isAbstract(m.getModifiers())) {
						String signature = getSignature(cc, m);
						m.insertBefore("org.stagemonitor.requestmonitor.profiler.Profiler.start(\"" + signature + "\");");
						m.insertAfter("org.stagemonitor.requestmonitor.profiler.Profiler.stop();", true);
					}
				}
				return cc.toBytecode();
			} finally {
				cc.detach();
			}
		} catch (Throwable ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
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

	private boolean isIncluded(String className) {
		// no includes -> include all
		boolean instrument = includes.isEmpty();
		for (String include : includes) {
			if (className.startsWith(include)) {
				return !hasMoreSpecificExclude(include);
			}
		}
		if (!instrument) {
			return false;
		}
		for (String exclude : excludes) {
			if (className.startsWith(exclude)) {
				return false;
			}
		}
		for (String exclude : excludeContaining) {
			if (className.contains(exclude)) {
				return false;
			}
		}
		return instrument;
	}

	private boolean hasMoreSpecificExclude(String include) {
		for (String exclude : excludes) {
			if (exclude.length() > include.length() && exclude.startsWith(include)) {
				return true;
			}
		}
		return false;
	}
}
