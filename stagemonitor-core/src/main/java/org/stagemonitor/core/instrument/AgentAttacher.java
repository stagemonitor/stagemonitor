package org.stagemonitor.core.instrument;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isBootstrapClassLoader;
import static net.bytebuddy.matcher.ElementMatchers.nameContains;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
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
import net.bytebuddy.description.NamedElement;
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

	private static final ElementMatcher.Junction<NamedElement> excludeTypes = nameStartsWith("java")
			.or(nameStartsWith("com.sun"))
			.or(nameStartsWith("jdk"))
			.or(nameStartsWith("org.aspectj"))
			.or(nameStartsWith("org.groovy"))
			.or(nameStartsWith("org.hibernate"))
			.or(nameStartsWith("oracle"))
			.or(not(nameContains("DataSource"))
					.and(nameStartsWith("org.apache.catalina")
							.or(nameStartsWith("org.apache.tomcat"))
							.or(nameStartsWith("org.jboss"))
							.or(nameStartsWith("org.wildfly"))
							.or(nameStartsWith("org.glassfish"))))
			.or(nameContains("javassist"))
			.or(nameContains("asm"));

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
			classFileTransformers.addAll(initByteBuddyClassFileTransformers());
			logger.debug("Attached agents in {} ms", System.currentTimeMillis() - start);
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
			final String transformerName = stagemonitorByteBuddyTransformer.getClass().getSimpleName();
			if (stagemonitorByteBuddyTransformer.isActive() && !isExcluded(transformerName)) {
				try {
					final long start = System.currentTimeMillis();
					classFileTransformers.add(installClassFileTransformer(stagemonitorByteBuddyTransformer, transformerName));
					logger.debug("Attached {} in {} ms", transformerName, System.currentTimeMillis() - start);
				} catch (Exception e) {
					logger.warn("Error while installing " + transformerName, e);
				}
			}
		}
		return classFileTransformers;
	}

	private static ClassFileTransformer installClassFileTransformer(StagemonitorByteBuddyTransformer stagemonitorByteBuddyTransformer, String transformerName) {
		return new AgentBuilder.Default(new ByteBuddy().with(TypeValidation.of(corePlugin.isDebugInstrumentation())))
				.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
				.with(stagemonitorByteBuddyTransformer)
				.ignore(any(), isBootstrapClassLoader()).or(excludeTypes)
				.disableClassFormatChanges()
				.type(stagemonitorByteBuddyTransformer.getTypeMatcher(),
						stagemonitorByteBuddyTransformer.getClassLoaderMatcher()
								.and(not(new IsIgnoredClassLoaderElementMatcher())))
				.transform(stagemonitorByteBuddyTransformer.getTransformer())
				.installOn(instrumentation);
	}

	private static boolean isExcluded(String transformerName) {
		return corePlugin.getExcludedInstrumenters().contains(transformerName);
	}

	private static class IsIgnoredClassLoaderElementMatcher implements ElementMatcher<ClassLoader> {
		@Override
		public boolean matches(ClassLoader target) {
			return hashCodesOfClassLoadersToIgnore.contains(System.identityHashCode(target));
		}
	}

}
