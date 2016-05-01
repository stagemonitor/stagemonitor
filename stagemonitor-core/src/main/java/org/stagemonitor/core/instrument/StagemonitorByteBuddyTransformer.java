package org.stagemonitor.core.instrument;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isFinal;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.isNative;
import static net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static net.bytebuddy.matcher.ElementMatchers.isTypeInitializer;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.stagemonitor.core.instrument.CachedClassLoaderMatcher.cached;
import static org.stagemonitor.core.instrument.StagemonitorClassNameMatcher.isInsideMonitoredProject;
import static org.stagemonitor.core.instrument.TimedElementMatcherDecorator.timed;

import java.lang.annotation.Annotation;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.List;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;

public abstract class StagemonitorByteBuddyTransformer {

	protected static final Configuration configuration = Stagemonitor.getConfiguration();

	protected static final boolean DEBUG_INSTRUMENTATION = configuration.getConfig(CorePlugin.class).isDebugInstrumentation();

	private static final Logger logger = LoggerFactory.getLogger(StagemonitorByteBuddyTransformer.class);

	private static final ElementMatcher.Junction<ClassLoader> applicationClassLoaderMatcher = cached(new ApplicationClassLoaderMatcher());

	protected final String transformerName = getClass().getSimpleName();

	public final AgentBuilder.RawMatcher getMatcher() {
		return new AgentBuilder.RawMatcher() {
			@Override
			public boolean matches(TypeDescription typeDescription, ClassLoader classLoader, Class<?> classBeingRedefined, ProtectionDomain protectionDomain) {
				final boolean matches = timed("classloader", "any", getClassLoaderMatcher()).matches(classLoader) &&
						timed("type", "any", getTypeMatcher()).matches(typeDescription) &&
						getRawMatcher().matches(typeDescription, classLoader, classBeingRedefined, protectionDomain);
				if (!matches) {
					onIgnored(typeDescription, classLoader);
				}
				return matches;
			}
		};
	}

	protected AgentBuilder.RawMatcher getRawMatcher() {
		return new AgentBuilder.RawMatcher() {
			@Override
			public boolean matches(TypeDescription typeDescription, ClassLoader classLoader, Class<?> classBeingRedefined, ProtectionDomain protectionDomain) {
				return true;
			}
		};
	}

	protected ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
		return getIncludeTypeMatcher()
				.and(not(isInterface()))
				.and(not(isSynthetic()))
				.and(not(getExtraExcludeTypeMatcher()));
	}

	public boolean isActive() {
		return true;
	}

	protected ElementMatcher.Junction<TypeDescription> getIncludeTypeMatcher() {
		return isInsideMonitoredProject().or(getExtraIncludeTypeMatcher());
	}

	protected ElementMatcher.Junction<TypeDescription> getExtraIncludeTypeMatcher() {
		return none();
	}

	protected ElementMatcher.Junction<TypeDescription> getExtraExcludeTypeMatcher() {
		return none();
	}

	protected ElementMatcher.Junction<ClassLoader> getClassLoaderMatcher() {
		return applicationClassLoaderMatcher;
	}

	public AgentBuilder.Transformer getTransformer() {
		return new AgentBuilder.Transformer() {

			private AsmVisitorWrapper.ForDeclaredMethods advice = registerDynamicValues()
					.to(getAdviceClass())
					.on(timed("method", transformerName, getMethodElementMatcher()));

			@Override
			public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
				beforeTransformation(typeDescription, classLoader);
				return builder.visit(advice);
			}

		};
	}

	private Advice.WithCustomMapping registerDynamicValues() {
		List<StagemonitorDynamicValue<?>> dynamicValues = getDynamicValues();
		Advice.WithCustomMapping withCustomMapping = Advice.withCustomMapping();
		for (StagemonitorDynamicValue dynamicValue : dynamicValues) {
			withCustomMapping = withCustomMapping.bind(dynamicValue.getAnnotationClass(), dynamicValue);
		}
		return withCustomMapping;
	}

	protected List<StagemonitorDynamicValue<?>> getDynamicValues() {
		return Collections.emptyList();
	}

	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getMethodElementMatcher() {
		return not(isConstructor())
				.and(not(isAbstract()))
				.and(not(isNative()))
				.and(not(isFinal()))
				.and(not(isSynthetic()))
				.and(not(isTypeInitializer()))
				.and(getExtraMethodElementMatcher());
	}

	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return any();
	}

	protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
		return getClass();
	}

	public abstract static class StagemonitorDynamicValue<T extends Annotation> implements Advice.DynamicValue<T> {
		public abstract Class<T> getAnnotationClass();
	}

	/**
	 * Returns the order of this transformer when multiple transformers match a method.
	 * </p>
	 * Higher orders will be applied first
	 *
	 * @return the order
	 */
	protected int getOrder() {
		return 0;
	}

	/**
	 * This method is called before the transformation.
	 * You can stop the transformation from happening by returning false from this method.
	 *
	 * @param typeDescription The type that is being transformed.
	 * @param classLoader     The class loader which is loading this type.
	 * @return <code>true</code> to proceed with the transformation, <code>false</code> to stop this transformation
	 */
	public void beforeTransformation(TypeDescription typeDescription, ClassLoader classLoader) {
		if (DEBUG_INSTRUMENTATION && logger.isDebugEnabled()) {
			logger.debug("TRANSFORM {} ({})", typeDescription.getName(), getClass().getSimpleName());
		}
	}

	public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader) {
		if (DEBUG_INSTRUMENTATION && logger.isDebugEnabled() && getTypeMatcher().matches(typeDescription)) {
			logger.debug("IGNORE {} ({})", typeDescription.getName(), getClass().getSimpleName());
		}
	}

}
