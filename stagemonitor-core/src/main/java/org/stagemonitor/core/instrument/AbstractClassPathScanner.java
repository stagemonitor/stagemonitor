package org.stagemonitor.core.instrument;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

/**
 * This transformer does not modify classes but only searches for matching {@link TypeDescription} and {@link MethodDescription}s
 */
public abstract class AbstractClassPathScanner extends StagemonitorByteBuddyTransformer {

	@Override
	public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader) {
		onTypeMatch(typeDescription);
	}

	protected void onTypeMatch(TypeDescription typeDescription) {
		for (MethodDescription.InDefinedShape methodDescription : typeDescription.getDeclaredMethods()
				.filter(getMethodElementMatcher())) {
			onMethodMatch(methodDescription);
		}
	}

	protected abstract void onMethodMatch(MethodDescription.InDefinedShape methodDescription);

	@Override
	protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
		return null;
	}
}
