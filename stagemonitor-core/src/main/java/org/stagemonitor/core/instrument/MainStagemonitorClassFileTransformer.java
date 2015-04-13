package org.stagemonitor.core.instrument;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ServiceLoader;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.core.Stagemonitor;

public class MainStagemonitorClassFileTransformer implements ClassFileTransformer {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private Iterable<StagemonitorJavassistInstrumenter> instrumenters;

	public Collection<String> includes;

	public Collection<String> excludes;

	public Collection<String> excludeContaining;

	public MainStagemonitorClassFileTransformer() {
		instrumenters = ServiceLoader.load(StagemonitorJavassistInstrumenter.class);
		try {
			for (Object instrumenter : instrumenters) {
				logger.info("Registering " + instrumenter.getClass().getSimpleName());
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		initIncludesAndExcludes();
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
							ProtectionDomain protectionDomain, byte[] classfileBuffer)
			throws IllegalClassFormatException {
		if (loader == null || StringUtils.isEmpty(className)) {
			return classfileBuffer;
		}
		try {
			if (isIncluded(className)) {
				classfileBuffer = transformIncluded(loader, classfileBuffer);
			} else {
				classfileBuffer = transformOther(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return classfileBuffer;
	}

	private byte[] transformIncluded(ClassLoader loader, byte[] classfileBuffer) throws Exception {
		CtClass ctClass = getCtClass(loader, classfileBuffer);
		try {
			for (StagemonitorJavassistInstrumenter instrumenter : instrumenters) {
				try {
					instrumenter.transformIncludedClass(ctClass);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			classfileBuffer = ctClass.toBytecode();
		} finally {
			ctClass.detach();
		}

		return classfileBuffer;
	}

	public static CtClass getCtClass(ClassLoader loader, byte[] classfileBuffer) throws IOException {
		ClassPool classPool = ClassPool.getDefault();
		classPool.insertClassPath(new LoaderClassPath(loader));
		return classPool.makeClass(new java.io.ByteArrayInputStream(classfileBuffer), false);
	}

	private byte[] transformOther(ClassLoader loader, String className, Class<?> classBeingRedefined,
								  ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		for (StagemonitorJavassistInstrumenter instrumenter : instrumenters) {
			try {
				classfileBuffer = instrumenter.transformOtherClass(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
		return classfileBuffer;
	}

	private boolean isIncluded(String className) {
		for (String exclude : excludeContaining) {
			if (className.contains(exclude)) {
				return false;
			}
		}

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
		CorePlugin requestMonitorPlugin = Stagemonitor.getConfiguration(CorePlugin.class);

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
