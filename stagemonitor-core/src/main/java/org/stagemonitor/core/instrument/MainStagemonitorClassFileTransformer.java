package org.stagemonitor.core.instrument;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
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

	private static final Logger logger = LoggerFactory.getLogger(MainStagemonitorClassFileTransformer.class);

	private Iterable<StagemonitorJavassistInstrumenter> instrumenters;
	private static MetricRegistry metricRegistry;
	private static CorePlugin corePlugin;

	public MainStagemonitorClassFileTransformer() {
		metricRegistry = Stagemonitor.getMetricRegistry();
		corePlugin = Stagemonitor.getConfiguration(CorePlugin.class);
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
		if (StagemonitorAgent.getInstrumentation() != null) {
			// already initialized via -javaagent
			return;
		}
		metricRegistry = Stagemonitor.getMetricRegistry();
		corePlugin = Stagemonitor.getConfiguration(CorePlugin.class);

		final Timer.Context time = metricRegistry.timer("internal.transform.performRuntimeAttachment").time();
		try {
			Instrumentation instrumentation = AgentLoader.loadAgent();
			long start = System.currentTimeMillis();
			final MainStagemonitorClassFileTransformer transformer = new MainStagemonitorClassFileTransformer();
			instrumentation.addTransformer(transformer, true);
			List<Class<?>> classesToRetransform = new LinkedList<Class<?>>();
			for (Class loadedClass : instrumentation.getAllLoadedClasses()) {
				final boolean included = transformer.isIncluded(loadedClass.getName().replace(".", "/"));
				if (included && instrumentation.isModifiableClass(loadedClass)) {
					classesToRetransform.add(loadedClass);
				}
			}
			retransformClasses(instrumentation, classesToRetransform, transformer);
			logger.info("Retransformed {} classes in {} ms", classesToRetransform.size(), System.currentTimeMillis() - start);
		} catch (Exception e) {
			logger.warn("Failed to perform runtime attachment of the stagemonitor agent. " +
					"You can load the agent with the command line argument -javaagent:/path/to/stagemonitor-javaagent-<version>.jar", e);
		}
		if (corePlugin.isInternalMonitoringActive()) {
			time.stop();
		}
	}

	private static void retransformClasses(Instrumentation instrumentation, List<Class<?>> classesToRetransform, MainStagemonitorClassFileTransformer transformer) {
		if (classesToRetransform.isEmpty()) {
			return;
		}

		try {
			final Timer.Context timeretransformClasses = metricRegistry.timer("internal.transform.retransformClasses").time();
			instrumentation.retransformClasses(classesToRetransform.toArray(new Class[classesToRetransform.size()]));
			if (corePlugin.isInternalMonitoringActive()) {
				timeretransformClasses.stop();
			}
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
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
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}
		if (corePlugin.isInternalMonitoringActive()) {
			time.stop();
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

	private byte[] transformWithJavassist(ClassLoader loader, byte[] classfileBuffer, String className) throws Exception {
		final Timer.Context time = metricRegistry.timer("internal.transform.javassist.All").time();
		CtClass ctClass = getCtClass(loader, classfileBuffer, className);
		try {
			for (StagemonitorJavassistInstrumenter instrumenter : instrumenters) {
				if (instrumenter.isIncluded(className)) {
					try {
						final Timer.Context timeTransfomer = metricRegistry.timer("internal.transform.javassist." + instrumenter.getClass().getSimpleName()).time();
						instrumenter.transformClass(ctClass, loader);
						if (corePlugin.isInternalMonitoringActive()) {
							timeTransfomer.stop();
						}
					} catch (Exception e) {
						logger.warn(e.getMessage(), e);
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
		return classPool.get(className.replace('/', '.'));
	}

}
