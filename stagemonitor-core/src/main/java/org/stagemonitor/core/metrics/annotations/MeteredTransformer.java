package org.stagemonitor.core.metrics.annotations;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;

import com.codahale.metrics.annotation.Metered;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;

/**
 * Implementation for the {@link Metered} annotation
 */
public class MeteredTransformer extends StagemonitorByteBuddyTransformer {

	@Override
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return isAnnotatedWith(Metered.class);
	}

	@Advice.OnMethodEnter
	public static void meter(@MeteredSignature String signature) {
		Stagemonitor.getMetric2Registry().meter(name("rate").tag("signature", signature).build()).mark();
	}

	@Override
	protected List<StagemonitorDynamicValue<?>> getDynamicValues() {
		return Collections.<StagemonitorDynamicValue<?>>singletonList(new MeteredSignatureDynamicValue());
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface MeteredSignature {
	}

	public static class MeteredSignatureDynamicValue extends MetricAnnotationSignatureDynamicValue<MeteredSignature> {

		protected NamingParameters getNamingParameters(MethodDescription.InDefinedShape instrumentedMethod) {
			final Metered metered = instrumentedMethod.getDeclaredAnnotations().ofType(Metered.class).loadSilent();
			return new NamingParameters(metered.name(), metered.absolute());
		}

		@Override
		public Class<MeteredSignature> getAnnotationClass() {
			return MeteredSignature.class;
		}
	}
}
