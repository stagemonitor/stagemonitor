package org.stagemonitor.core.instrument;

import static net.bytebuddy.matcher.ElementMatchers.none;

import java.util.Collections;
import java.util.List;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * This transformer does not modify classes but only searches for matching {@link TypeDescription} and {@link MethodDescription}s
 */
public abstract class AbstractClassPathScanner extends StagemonitorByteBuddyTransformer {

	@Override
	public final AgentBuilder.Transformer getTransformer() {
		return AgentBuilder.Transformer.NoOp.INSTANCE;
	}

	@Override
	public final ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
		return none();
	}

	@Override
	public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader) {
		if (super.getTypeMatcher().matches(typeDescription) && getClassLoaderMatcher().matches(classLoader)) {
			onTypeMatch(typeDescription);
		}
	}

	protected void onTypeMatch(TypeDescription typeDescription) {
		for (MethodDescription.InDefinedShape methodDescription : typeDescription.getDeclaredMethods()
				.filter(getMethodElementMatcher())) {
			onMethodMatch(methodDescription);
		}
	}

	protected abstract void onMethodMatch(MethodDescription.InDefinedShape methodDescription);

	@Override
	protected final List<StagemonitorDynamicValue<?>> getDynamicValues() {
		return Collections.emptyList();
	}

	@Override
	protected final Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
		return super.getAdviceClass();
	}
}
