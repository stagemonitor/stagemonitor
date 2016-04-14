package org.stagemonitor.core.instrument;

import static net.bytebuddy.matcher.ElementMatchers.not;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;

/**
 * Attaches the {@link ByteBuddyAgent} at runtime and registers all {@link StagemonitorByteBuddyTransformer}s
 */
public class AgentAttacher {

	private static final Runnable NOOP_ON_SHUTDOWN_ACTION = new Runnable() {
		public void run() {
		}
	};
	private static final Logger logger = LoggerFactory.getLogger(AgentAttacher.class);
	private static final String IGNORED_CLASSLOADERS_KEY = AgentAttacher.class.getName() + "hashCodesOfClassLoadersToIgnore";

	private static CorePlugin corePlugin = Stagemonitor.getPlugin(CorePlugin.class);
	private static boolean runtimeAttached = false;
	private static Set<Integer> hashCodesOfClassLoadersToIgnore = new HashSet<Integer>();
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
			classFileTransformers.addAll(initByteBuddyClassFileTransformers());
		}
		return new Runnable() {
			public void run() {
				for (ClassFileTransformer classFileTransformer : classFileTransformers) {
					instrumentation.removeTransformer(classFileTransformer);
				}
				final int classLoaderHash = System.identityHashCode(AgentAttacher.class.getClassLoader());
				// This ClassLoader is shutting down so don't try to retransform classes of it in the future
				hashCodesOfClassLoadersToIgnore.add(classLoaderHash);
			}
		};
	}

	private static boolean initInstrumentation() {
		try {
			try {
				instrumentation = ByteBuddyAgent.getInstrumentation();
			} catch (IllegalStateException e) {
				instrumentation = ByteBuddyAgent.install();
			}
			Dispatcher.init(instrumentation);
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

	private static List<ClassFileTransformer> initByteBuddyClassFileTransformers() {
		List<ClassFileTransformer> classFileTransformers = new ArrayList<ClassFileTransformer>();
		final ServiceLoader<StagemonitorByteBuddyTransformer> loader = ServiceLoader.load(StagemonitorByteBuddyTransformer.class, Stagemonitor.class.getClassLoader());

		for (StagemonitorByteBuddyTransformer stagemonitorByteBuddyTransformer : loader) {
			if (stagemonitorByteBuddyTransformer.isActive() && !isExcluded(stagemonitorByteBuddyTransformer)) {
				logger.info("Registering " + stagemonitorByteBuddyTransformer.getClass().getSimpleName());
				try {
					classFileTransformers.add(installClassFileTransformer(stagemonitorByteBuddyTransformer));
				} catch (Exception e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}
		return classFileTransformers;
	}

	private static ClassFileTransformer installClassFileTransformer(StagemonitorByteBuddyTransformer stagemonitorByteBuddyTransformer) {
		return new AgentBuilder.Default(new ByteBuddy().with(TypeValidation.of(corePlugin.isDebugInstrumentation())))
							.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
							.with(stagemonitorByteBuddyTransformer)
							.disableClassFormatChanges()
							.type(stagemonitorByteBuddyTransformer.getTypeMatcher(), stagemonitorByteBuddyTransformer.getClassLoaderMatcher()
									.and(not(new IsIgnoredClassLoaderElementMatcher())))
							.transform(stagemonitorByteBuddyTransformer.getTransformer())
							.installOn(instrumentation);
	}

	private static boolean isExcluded(StagemonitorByteBuddyTransformer stagemonitorByteBuddyTransformer) {
		return corePlugin.getExcludedInstrumenters().contains(stagemonitorByteBuddyTransformer.getClass().getSimpleName());
	}

	private static class IsIgnoredClassLoaderElementMatcher implements ElementMatcher<ClassLoader> {
		@Override
		public boolean matches(ClassLoader target) {
			return hashCodesOfClassLoadersToIgnore.contains(System.identityHashCode(target));
		}
	}
}
