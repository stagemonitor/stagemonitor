package org.stagemonitor.core.instrument;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;

/**
 * This transformer does not modify classes but only searches for matching {@link TypeDescription} and {@link MethodDescription}s
 */
public abstract class AbstractClassPathScanner extends StagemonitorByteBuddyTransformer {

	@Override
	public AgentBuilder.Transformer getTransformer() {
		return new AgentBuilder.Transformer() {
			@Override
			public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
				onTypeMatch(typeDescription);
				return builder;
			}
		};
	}

	protected void onTypeMatch(TypeDescription typeDescription) {
		for (MethodDescription.InDefinedShape methodDescription : typeDescription.getDeclaredMethods()
				.filter(getMethodElementMatcher())) {
			onMethodMatch(methodDescription);
		}
	}

	protected abstract void onMethodMatch(MethodDescription.InDefinedShape methodDescription);

}
