package org.stagemonitor.core.instrument;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isBootstrapClassLoader;
import static net.bytebuddy.matcher.ElementMatchers.nameContains;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.stagemonitor.core.instrument.ClassLoaderNameMatcher.classLoaderWithName;
import static org.stagemonitor.core.instrument.ClassLoaderNameMatcher.isReflectionClassLoader;
import static org.stagemonitor.core.instrument.TimedElementMatcherDecorator.timed;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.stagemonitor.dispatcher.Dispatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.core.util.IOUtils;

/**
 * Attaches the {@link ByteBuddyAgent} at runtime and registers all {@link StagemonitorByteBuddyTransformer}s
 */
public class AgentAttacher {

	private static final Logger logger = LoggerFactory.getLogger(AgentAttacher.class);
	private static final String DISPATCHER_CLASS_NAME = "java.lang.stagemonitor.dispatcher.Dispatcher";
	private static final String IGNORED_CLASSLOADERS_KEY = AgentAttacher.class.getName() + "hashCodesOfClassLoadersToIgnore";
	private static final DeactivatableCachingBinaryLocator binaryLocator = new DeactivatableCachingBinaryLocator();
	private static final Runnable NOOP_ON_SHUTDOWN_ACTION = new Runnable() {
		public void run() {
		}
	};

	private static CorePlugin corePlugin = Stagemonitor.getPlugin(CorePlugin.class);
	private static boolean runtimeAttached = false;
	private static Set<String> hashCodesOfClassLoadersToIgnore;
	private static Instrumentation instrumentation;

	private AgentAttacher() {
	}

	/**
	 * Attaches the profiler and other instrumenters at runtime so that it is not necessary to add the
	 * -javaagent command line argument.
	 *
	 * @return A runnable that should be called on shutdown to unregister this class file transformer
	 */
	public static synchronized Runnable performRuntimeAttachment() {
		if (runtimeAttached || !corePlugin.isStagemonitorActive() || !corePlugin.isAttachAgentAtRuntime()) {
			return NOOP_ON_SHUTDOWN_ACTION;
		}
		runtimeAttached = true;

		final List<ClassFileTransformer> classFileTransformers = new ArrayList<ClassFileTransformer>();
		if (initInstrumentation()) {
			final long start = System.currentTimeMillis();
			classFileTransformers.add(initByteBuddyClassFileTransformer());
			if (corePlugin.isDebugInstrumentation()) {
				logger.info("Attached agents in {} ms", System.currentTimeMillis() - start);
			}
			TimedElementMatcherDecorator.logMetrics();
		}
		return new Runnable() {
			public void run() {
				for (ClassFileTransformer classFileTransformer : classFileTransformers) {
					instrumentation.removeTransformer(classFileTransformer);
				}
				// This ClassLoader is shutting down so don't try to retransform classes of it in the future
				hashCodesOfClassLoadersToIgnore.add(ClassUtils.getIdentityString(AgentAttacher.class.getClassLoader()));
				// it does not harm to clear the caches on shut down once again in case a ClassLoader slipped into the cache
				binaryLocator.deactivateCaching();
			}
		};
	}

	private static boolean initInstrumentation() {
		try {
			try {
				instrumentation = ByteBuddyAgent.getInstrumentation();
			} catch (IllegalStateException e) {
				instrumentation = ByteBuddyAgent.install(new ByteBuddyAgent.AttachmentProvider.Compound(new EhCacheAttachmentProvider(), ByteBuddyAgent.AttachmentProvider.DEFAULT));
			}
			ensureDispatcherIsAppendedToBootstrapClasspath(instrumentation);
			if (!Dispatcher.getValues().containsKey(IGNORED_CLASSLOADERS_KEY)) {
				Dispatcher.put(IGNORED_CLASSLOADERS_KEY, Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>()));
			}
			hashCodesOfClassLoadersToIgnore = Dispatcher.get(IGNORED_CLASSLOADERS_KEY);
			return true;
		} catch (Exception e) {
			logger.warn("Failed to perform runtime attachment of the stagemonitor agent. " +
					"You can try loadint the agent with the command line argument -javaagent:/path/to/byte-buddy-agent-<version>.jar", e);
			return false;
		}
	}

	private static Class<?> ensureDispatcherIsAppendedToBootstrapClasspath(Instrumentation instrumentation)
			throws ClassNotFoundException, IOException {
		final ClassLoader bootstrapClassloader = ClassLoader.getSystemClassLoader().getParent();
		try {
			return bootstrapClassloader.loadClass(DISPATCHER_CLASS_NAME);
			// already injected
		} catch (ClassNotFoundException e) {
			final JarFile jarfile = new JarFile(createTempDispatcherJar());
			instrumentation.appendToBootstrapClassLoaderSearch(jarfile);
			return bootstrapClassloader.loadClass(DISPATCHER_CLASS_NAME);
		}
	}

	private static File createTempDispatcherJar() throws IOException {
		final InputStream input = AgentAttacher.class.getClassLoader()
				.getResourceAsStream("stagemonitor-dispatcher.jar.gradlePleaseDontExtract");
		if (input == null) {
			throw new IllegalStateException("If you see this exception inside you IDE, you have to execute gradle " +
					"processResources before running the tests.");
		}
		final File tempDispatcherJar = File.createTempFile("stagemonitor-dispatcher", ".jar");
		tempDispatcherJar.deleteOnExit();
		IOUtils.copy(input, new FileOutputStream(tempDispatcherJar));
		return tempDispatcherJar;
	}

	private static ClassFileTransformer initByteBuddyClassFileTransformer() {
		AgentBuilder agentBuilder = createAgentBuilder();
		for (StagemonitorByteBuddyTransformer transformer : getStagemonitorByteBuddyTransformers()) {
			agentBuilder = agentBuilder
					.type(transformer.getMatcher())
					.transform(transformer.getTransformer())
					.asDecorator();
		}

		final long start = System.currentTimeMillis();
		try {
			return agentBuilder.installOn(instrumentation);
		} finally {
			if (corePlugin.isDebugInstrumentation()) {
				logger.info("Installed agent in {} ms", System.currentTimeMillis() - start);
			}
		}
	}

	private static AgentBuilder createAgentBuilder() {
		return new AgentBuilder.Default(new ByteBuddy().with(TypeValidation.of(corePlugin.isDebugInstrumentation())))
				.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
				.with(corePlugin.isDebugInstrumentation() ? new ErrorLoggingListener() : AgentBuilder.Listener.NoOp.INSTANCE)
				.with(binaryLocator)
				.ignore(any(), timed("classloader", "bootstrap", isBootstrapClassLoader()))
				.or(any(), timed("classloader", "reflection", isReflectionClassLoader()))
				.or(any(), timed("classloader", "groovy-call-site", classLoaderWithName("org.codehaus.groovy.runtime.callsite.CallSiteClassLoader")))
				.or(any(), new IsIgnoredClassLoaderElementMatcher())
				.or(timed("type", "global-exclude", nameStartsWith("java")
						.or(nameStartsWith("com.sun."))
						.or(nameStartsWith("sun."))
						.or(nameStartsWith("jdk."))
						.or(nameStartsWith("org.aspectj."))
						.or(nameStartsWith("org.groovy."))
						.or(nameStartsWith("com.p6spy."))
						.or(nameStartsWith("net.bytebuddy."))
						.or(nameStartsWith("org.slf4j.").and(not(nameStartsWith("org.slf4j.impl."))))
						.or(nameContains("javassist"))
						.or(nameContains(".asm."))
						.or(nameStartsWith("org.stagemonitor")
								.and(not(nameContains("Test").or(nameContains("benchmark")))))
				))
				.disableClassFormatChanges();
	}

	private static Iterable<StagemonitorByteBuddyTransformer> getStagemonitorByteBuddyTransformers() {
		List<StagemonitorByteBuddyTransformer> transformers = new ArrayList<StagemonitorByteBuddyTransformer>();
		for (StagemonitorByteBuddyTransformer transformer : ServiceLoader.load(StagemonitorByteBuddyTransformer.class, Stagemonitor.class.getClassLoader())) {
			if (transformer.isActive() && !isExcluded(transformer)) {
				transformers.add(transformer);
				if (corePlugin.isDebugInstrumentation()) {
					logger.info("Registering {}", transformer.getClass().getSimpleName());
				}
			} else if (corePlugin.isDebugInstrumentation()) {
				logger.info("Excluding {}", transformer.getClass().getSimpleName());
			}
		}
		Collections.sort(transformers, new Comparator<StagemonitorByteBuddyTransformer>() {
			@Override
			public int compare(StagemonitorByteBuddyTransformer o1, StagemonitorByteBuddyTransformer o2) {
				return o1.getOrder() > o2.getOrder() ? -1 : 1;
			}
		});
		return transformers;
	}

	private static <T> ElementMatcher.Junction<T> matchesAny(List<ElementMatcher.Junction<T>> matchers) {
		// a neat little way to optimize the performance: if we have two equal matchers, we can discard one
		final HashSet<ElementMatcher.Junction<T>> deduplicated = new HashSet<ElementMatcher.Junction<T>>(matchers);
		ElementMatcher.Junction<T> result = none();
		for (ElementMatcher.Junction<T> matcher : deduplicated) {
			result = result.or(matcher);
		}
		return result;
	}

	private static boolean isExcluded(StagemonitorByteBuddyTransformer transformer) {
		return corePlugin.getExcludedInstrumenters().contains(transformer.getClass().getSimpleName());
	}

	/**
	 * This method should be called as soon as most classes are loaded.
	 * <p/>
	 * It disables caching of {@link net.bytebuddy.description.type.TypeDescription}s which makes further
	 * transformations a bit slower but also clears the underlying cache which frees resources.
	 */
	public static void onMostClassesLoaded() {
		TimedElementMatcherDecorator.logMetrics();
		binaryLocator.deactivateCaching();
	}

	private static class IsIgnoredClassLoaderElementMatcher implements ElementMatcher<ClassLoader> {
		@Override
		public boolean matches(ClassLoader target) {
			return hashCodesOfClassLoadersToIgnore.contains(ClassUtils.getIdentityString(target));
		}
	}

}
