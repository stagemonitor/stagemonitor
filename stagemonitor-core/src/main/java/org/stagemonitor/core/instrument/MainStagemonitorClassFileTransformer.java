package org.stagemonitor.core.instrument;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ServiceLoader;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import org.stagemonitor.core.CorePlugin;

public class MainStagemonitorClassFileTransformer implements ClassFileTransformer {

	private Iterable<StagemonitorJavassistInstrumenter> instrumenters;

	public Collection<String> includes;

	public Collection<String> excludes;

	public Collection<String> excludeContaining;

	public MainStagemonitorClassFileTransformer() {
		CorePlugin.getSimpleInstance();
		instrumenters = ServiceLoader.load(StagemonitorJavassistInstrumenter.class);
		for (Object classFileTransformer : instrumenters) {
			System.out.println("Registering " + classFileTransformer.getClass().getSimpleName());
		}
		initIncludesAndExcludes();
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
							ProtectionDomain protectionDomain, byte[] classfileBuffer)
			throws IllegalClassFormatException {
		if (loader == null) {
			return classfileBuffer;
		}
		if (isIncluded(className)) {
			classfileBuffer = transformIncluded(loader, classfileBuffer);
		} else {
			classfileBuffer = transformOther(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
		}
		return classfileBuffer;
	}

	private byte[] transformIncluded(ClassLoader loader, byte[] classfileBuffer) {
		try {
			ClassPool classPool = ClassPool.getDefault();
			classPool.insertClassPath(new LoaderClassPath(loader));
			CtClass ctClass = classPool.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));
			try {
				for (StagemonitorJavassistInstrumenter instrumenter : instrumenters) {
					instrumenter.transformIncludedClass(ctClass);
				}
				classfileBuffer = ctClass.toBytecode();
			} finally {
				ctClass.detach();
			}
		} catch (Throwable ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		return classfileBuffer;
	}

	private byte[] transformOther(ClassLoader loader, String className, Class<?> classBeingRedefined,
								  ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		for (StagemonitorJavassistInstrumenter instrumenter : instrumenters) {
			classfileBuffer = instrumenter.transformOtherClass(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
		}
		return classfileBuffer;
	}

	private boolean isIncluded(String className) {
		// no includes -> include all
		boolean instrument = includes.isEmpty();
		for (String include : includes) {
			if (className.startsWith(include)) {
				return !hasMoreSpecificExclude(className, include);
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

	private boolean hasMoreSpecificExclude(String className, String include) {
		for (String exclude : excludes) {
			if (exclude.length() > include.length() && exclude.startsWith(include) && className.startsWith(exclude)) {
				return true;
			}
		}
		return false;
	}

	private void initIncludesAndExcludes() {
		CorePlugin requestMonitorPlugin = CorePlugin.getSimpleInstance();

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

}
