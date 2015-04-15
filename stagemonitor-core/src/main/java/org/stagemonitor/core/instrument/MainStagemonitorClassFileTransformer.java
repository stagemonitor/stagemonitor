package org.stagemonitor.core.instrument;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ServiceLoader;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.agent.StagemonitorAgent;
import org.stagemonitor.core.util.StringUtils;

public class MainStagemonitorClassFileTransformer implements ClassFileTransformer {

	private static final Logger logger = LoggerFactory.getLogger(MainStagemonitorClassFileTransformer.class);

	private Iterable<StagemonitorJavassistInstrumenter> instrumenters;

	public MainStagemonitorClassFileTransformer() {
		instrumenters = ServiceLoader.load(StagemonitorJavassistInstrumenter.class);
		try {
			for (Object instrumenter : instrumenters) {
				logger.info("Registering " + instrumenter.getClass().getSimpleName());
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * Attaches the profiler and other instrumenters at runtime so that it is not necessary to add the
	 * -javaagent command line argument.
	 */
	public static void performRuntimeAttachment() {
		if (StagemonitorAgent.isInitializedViaJavaagent()) {
			return;
		}
		try {
			Instrumentation instrumentation = AgentLoader.loadAgent();
			final MainStagemonitorClassFileTransformer transformer = new MainStagemonitorClassFileTransformer();
			instrumentation.addTransformer(transformer, true);
			long start = System.currentTimeMillis();
			for (Class loadedClass : instrumentation.getAllLoadedClasses()) {
				if (transformer.isIncluded(loadedClass.getName().replace(".", "/"))) {
					try {
						instrumentation.retransformClasses(loadedClass);
					} catch (UnmodifiableClassException e) {
						logger.warn(e.getMessage(), e);
					}
				}
			}
			logger.info("Retransformed classes in {} ms", System.currentTimeMillis() - start);
		} catch (Exception e) {
			logger.warn("Failed to perform runtime attachment of the stagemonitor agent. " +
					"You can load the agent with the command line argument -javaagent:/path/to/stagemonitor-javaagent-<version>.jar", e);
		}
	}

	@Override
	public synchronized byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
							ProtectionDomain protectionDomain, byte[] classfileBuffer)
			throws IllegalClassFormatException {

		if (loader == null || StringUtils.isEmpty(className)) {
			return classfileBuffer;
		}
		try {
			if (isIncluded(className)) {
				classfileBuffer = transform(loader, classfileBuffer, className);
			}
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}
		return classfileBuffer;
	}

	private boolean isIncluded(String className) {
		for (StagemonitorJavassistInstrumenter instrumenter : instrumenters) {
			if (instrumenter.isIncluded(className)) {
				return true;
			}
		}
		return false;
	}

	private byte[] transform(ClassLoader loader, byte[] classfileBuffer, String className) throws Exception {
		CtClass ctClass = getCtClass(loader, classfileBuffer, className);
		try {
			for (StagemonitorJavassistInstrumenter instrumenter : instrumenters) {
				if (instrumenter.isIncluded(className)) {
					try {
						instrumenter.transformClass(ctClass, loader);
					} catch (Exception e) {
						logger.warn(e.getMessage(), e);
					}
				}
			}
			classfileBuffer = ctClass.toBytecode();
		} finally {
			ctClass.detach();
		}

		return classfileBuffer;
	}

	public CtClass getCtClass(ClassLoader loader, byte[] classfileBuffer, String className) throws Exception {
		ClassPool classPool = ClassPool.getDefault();
		classPool.insertClassPath(new LoaderClassPath(loader));
		return classPool.get(className.replace('/', '.'));
	}

}
