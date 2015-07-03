package org.stagemonitor.core.instrument;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.agent.StagemonitorAgent;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.StringUtils;

public class MainStagemonitorClassFileTransformer implements ClassFileTransformer {

	private static final Runnable NOOP_ON_SHUTDOWN_ACTION = new Runnable() { public void run() {} };
	private static final Logger logger = LoggerFactory.getLogger(MainStagemonitorClassFileTransformer.class);

	private List<StagemonitorJavassistInstrumenter> instrumenters = new ArrayList<StagemonitorJavassistInstrumenter>();
	private static MetricRegistry metricRegistry;
	private static CorePlugin corePlugin;
	private static boolean runtimeAttached = false;

	public MainStagemonitorClassFileTransformer() {
		metricRegistry = Stagemonitor.getMetricRegistry();
		corePlugin = Stagemonitor.getConfiguration(CorePlugin.class);
		try {
			final ServiceLoader<StagemonitorJavassistInstrumenter> loader = ServiceLoader
					.load(StagemonitorJavassistInstrumenter.class, Stagemonitor.class.getClassLoader());
			for (StagemonitorJavassistInstrumenter instrumenter : loader) {
				if (!corePlugin.getExcludedInstrumenters().contains(instrumenter.getClass().getSimpleName())) {
					logger.info("Registering " + instrumenter.getClass().getSimpleName());
					instrumenters.add(instrumenter);
				} else {
					logger.info("Not registering excluded " + instrumenter.getClass().getSimpleName());
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * Attaches the profiler and other instrumenters at runtime so that it is not necessary to add the
	 * -javaagent command line argument.
	 *
	 * @return A runnable that should be called on shutdown to unregister this class file transformer
	 */
	public static synchronized Runnable performRuntimeAttachment() {
		if (StagemonitorAgent.isInitializedViaJavaagent() || runtimeAttached) {
			return NOOP_ON_SHUTDOWN_ACTION;
		}
		runtimeAttached = true;
		metricRegistry = Stagemonitor.getMetricRegistry();
		corePlugin = Stagemonitor.getConfiguration(CorePlugin.class);
		if (!corePlugin.isStagemonitorActive() || !corePlugin.isAttachAgentAtRuntime()) {
			return NOOP_ON_SHUTDOWN_ACTION;
		}

		final Timer.Context time = metricRegistry.timer("internal.transform.performRuntimeAttachment").time();
		Runnable onShutdownAction = NOOP_ON_SHUTDOWN_ACTION;
		try {
			final Instrumentation instrumentation = AgentLoader.loadAgent();
			final long start = System.currentTimeMillis();
			final MainStagemonitorClassFileTransformer transformer = new MainStagemonitorClassFileTransformer();
			instrumentation.addTransformer(transformer, true);
			onShutdownAction = new Runnable() {
				public void run() {
					instrumentation.removeTransformer(transformer);
				}
			};

			List<Class<?>> classesToRetransform = new LinkedList<Class<?>>();
			for (Class loadedClass : instrumentation.getAllLoadedClasses()) {
				if (transformer.isRetransformClass(loadedClass, instrumentation)) {
					classesToRetransform.add(loadedClass);
				}
			}
			logger.info("Retransforming {} classes...", classesToRetransform.size());
			logger.debug("Classes to retransform: {}", classesToRetransform);
			retransformClasses(instrumentation, classesToRetransform);
			logger.info("Retransformed {} classes in {} ms", classesToRetransform.size(), System.currentTimeMillis() - start);
		} catch (Exception e) {
			logger.warn("Failed to perform runtime attachment of the stagemonitor agent. " +
					"You can load the agent with the command line argument -javaagent:/path/to/stagemonitor-javaagent-<version>.jar", e);
		}
		if (corePlugin.isInternalMonitoringActive()) {
			time.stop();
		}
		return onShutdownAction;
	}

	private static void retransformClasses(Instrumentation instrumentation, List<Class<?>> classesToRetransform) {
		if (classesToRetransform.isEmpty()) {
			return;
		}

		final Timer.Context timeretransformClasses = metricRegistry.timer("internal.transform.retransformClasses").time();
		for (Class<?> classToRetransform : classesToRetransform) {
			try {
				instrumentation.retransformClasses(classToRetransform);
			} catch (Throwable e) {
				logger.warn("Failed to retransform class {}", classToRetransform.getName());
				logger.debug(e.getMessage(), e);
			}
		}

//			instrumentation.retransformClasses(classesToRetransform.toArray(new Class[classesToRetransform.size()]));
		if (corePlugin.isInternalMonitoringActive()) {
			timeretransformClasses.stop();
		}
	}

	@Override
	public synchronized byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
										 ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

		final Timer.Context time = metricRegistry.timer("internal.transform.All").time();
		if (loader == null || StringUtils.isEmpty(className)) {
			return classfileBuffer;
		}
		try {
			if (isIncluded(className)) {
				classfileBuffer = transformWithJavassist(loader, classfileBuffer, className);
			}
		} catch (Throwable e) {
			logger.warn("Failed to transform class {}", className);
			logger.debug(e.getMessage(), e);
		}
		if (corePlugin.isInternalMonitoringActive()) {
			time.stop();
		}
		return classfileBuffer;
	}

	private boolean isRetransformClass(Class loadedClass, Instrumentation instrumentation) {
		return isTransformClassesOfClassLoader(loadedClass.getClassLoader()) &&
				!loadedClass.isInterface() &&
				instrumentation.isModifiableClass(loadedClass) &&
				isIncluded(loadedClass.getName().replace(".", "/"));
	}

	private boolean isIncluded(String className) {
		for (StagemonitorJavassistInstrumenter instrumenter : instrumenters) {
			if (instrumenter.isIncluded(className)) {
				return true;
			}
		}
		return false;
	}

	private boolean isTransformClassesOfClassLoader(ClassLoader loader) {
		for (StagemonitorJavassistInstrumenter instrumenter : instrumenters) {
			if (instrumenter.isTransformClassesOfClassLoader(loader)) {
				return true;
			}
		}
		return false;
	}

	private byte[] transformWithJavassist(ClassLoader loader, byte[] classfileBuffer, String className) throws Exception {
		final Timer.Context time = metricRegistry.timer("internal.transform.javassist.All").time();
		CtClass ctClass = getCtClass(loader, classfileBuffer, className);
		try {
			for (StagemonitorJavassistInstrumenter instrumenter : instrumenters) {
				if (instrumenter.isIncluded(className) && instrumenter.isTransformClassesOfClassLoader(loader)) {
					try {
						final Timer.Context timeTransfomer = metricRegistry.timer("internal.transform.javassist." + instrumenter.getClass().getSimpleName()).time();
						instrumenter.transformClass(ctClass, loader);
						if (corePlugin.isInternalMonitoringActive()) {
							timeTransfomer.stop();
						}
					} catch (Exception e) {
						logger.warn("An exception occured while transfroming class " + className +
								". This is usually nothing to worry about, because the class is just not instrumented", e);
					}
				}
			}
			classfileBuffer = ctClass.toBytecode();
		} finally {
			ctClass.detach();
		}
		if (corePlugin.isInternalMonitoringActive()) {
			time.stop();
		}
		return classfileBuffer;
	}

	public CtClass getCtClass(ClassLoader loader, byte[] classfileBuffer, String className) throws Exception {
		ClassPool classPool = ClassPool.getDefault();
		classPool.insertClassPath(new LoaderClassPath(loader));
		classPool.appendSystemPath();
		return classPool.get(className.replace('/', '.'));
	}

}
