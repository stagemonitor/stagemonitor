package org.stagemonitor.core.instrument;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import com.codahale.metrics.Timer;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.agent.StagemonitorAgent;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.StringUtils;

public class MainStagemonitorClassFileTransformer implements ClassFileTransformer {

	private static final Runnable NOOP_ON_SHUTDOWN_ACTION = new Runnable() { public void run() {} };
	private static final Logger logger = LoggerFactory.getLogger(MainStagemonitorClassFileTransformer.class);
	private static final String IGNORED_CLASSLOADERS_KEY = MainStagemonitorClassFileTransformer.class.getName() + "hashCodesOfClassLoadersToIgnore";

	private List<StagemonitorJavassistInstrumenter> instrumenters = new ArrayList<StagemonitorJavassistInstrumenter>();
	private static Metric2Registry metricRegistry;
	private static CorePlugin corePlugin;
	private static boolean runtimeAttached = false;
	private static Map<Integer, ClassPool> classPoolsByClassLoaderHash = new HashMap<Integer, ClassPool>();
	private static Set<Integer> hashCodesOfClassLoadersToIgnore = new HashSet<Integer>();

	public MainStagemonitorClassFileTransformer() {
		metricRegistry = Stagemonitor.getMetric2Registry();
		corePlugin = Stagemonitor.getConfiguration(CorePlugin.class);
		if (!System.getProperties().containsKey(IGNORED_CLASSLOADERS_KEY)) {
			System.getProperties().put(IGNORED_CLASSLOADERS_KEY, new HashSet<Integer>());
		}
		hashCodesOfClassLoadersToIgnore = (Set<Integer>) System.getProperties().get(IGNORED_CLASSLOADERS_KEY);
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
		metricRegistry = Stagemonitor.getMetric2Registry();
		corePlugin = Stagemonitor.getConfiguration(CorePlugin.class);
		if (!corePlugin.isStagemonitorActive() || !corePlugin.isAttachAgentAtRuntime()) {
			return NOOP_ON_SHUTDOWN_ACTION;
		}

		final Timer.Context time = metricRegistry.timer(name("internal_runtime_attachment_time").build()).time();
		Runnable onShutdownAction = NOOP_ON_SHUTDOWN_ACTION;
		try {
			final Instrumentation instrumentation = AgentLoader.loadAgent();
			final long start = System.currentTimeMillis();
			final MainStagemonitorClassFileTransformer transformer = new MainStagemonitorClassFileTransformer();
			instrumentation.addTransformer(transformer, true);
			onShutdownAction = new Runnable() {
				public void run() {
					instrumentation.removeTransformer(transformer);
					final int classLoaderHash = System.identityHashCode(MainStagemonitorClassFileTransformer.class.getClassLoader());
					// This ClassLoader is shutting down so don't try to retransform classes of it in the future
					hashCodesOfClassLoadersToIgnore.add(classLoaderHash);
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

		final Timer.Context timeretransformClasses = metricRegistry.timer(name("internal_retransform_classes_time").build()).time();
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

		if (loader == null || StringUtils.isEmpty(className)) {
			return classfileBuffer;
		}
		try {
			if (isIncluded(className, loader)) {
				classfileBuffer = transformWithJavassist(loader, classfileBuffer, className);
			}
		} catch (Throwable e) {
			logger.warn("Failed to transform class {}", className);
			logger.debug(e.getMessage(), e);
		}
		return classfileBuffer;
	}

	private boolean isRetransformClass(Class loadedClass, Instrumentation instrumentation) {
		final ClassLoader classLoader = loadedClass.getClassLoader();
		return !loadedClass.isInterface() &&
				instrumentation.isModifiableClass(loadedClass) &&
				!hashCodesOfClassLoadersToIgnore.contains(System.identityHashCode(classLoader)) &&
				isIncluded(loadedClass.getName().replace(".", "/"), classLoader);
	}

	private boolean isIncluded(String className, ClassLoader loader) {
		for (StagemonitorJavassistInstrumenter instrumenter : instrumenters) {
			if (instrumenter.isIncluded(className) && instrumenter.isTransformClassesOfClassLoader(loader)) {
				return true;
			}
		}
		return false;
	}

	private byte[] transformWithJavassist(ClassLoader loader, byte[] classfileBuffer, String className) throws Exception {
		CtClass ctClass = getCtClass(loader, classfileBuffer, className);
		try {
			for (StagemonitorJavassistInstrumenter instrumenter : instrumenters) {
				if (instrumenter.isIncluded(className) && instrumenter.isTransformClassesOfClassLoader(loader)) {
					try {
						final Timer.Context timeTransfomer = metricRegistry.timer(name("internal_retransform_classes_time")
								.type("javassist")
								.tag("instrumenter", instrumenter.getClass().getSimpleName())
								.build())
								.time();
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
		return classfileBuffer;
	}

	public CtClass getCtClass(ClassLoader loader, byte[] classfileBuffer, String className) throws Exception {
		final int classLoaderHash = System.identityHashCode(loader);
		ClassPool classPool;
		if (classPoolsByClassLoaderHash.containsKey(classLoaderHash)) {
			classPool = classPoolsByClassLoaderHash.get(classLoaderHash);
		} else {
			classPool = new ClassPool(true);
			classPool.insertClassPath(new LoaderClassPath(loader));
			classPoolsByClassLoaderHash.put(classLoaderHash, classPool);
		}
		return classPool.get(className.replace('/', '.'));
	}

}
