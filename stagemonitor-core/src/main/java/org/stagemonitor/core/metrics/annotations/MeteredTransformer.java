package org.stagemonitor.core.metrics.annotations;

import com.codahale.metrics.annotation.Metered;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.core.metrics.metrics2.MetricName;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

/**
 * Implementation for the {@link Metered} annotation
 */
public class MeteredTransformer extends StagemonitorByteBuddyTransformer {

	public static final MetricName.MetricNameTemplate metricNameTemplate = name("rate").templateFor("signature");

	@Override
	protected ElementMatcher.Junction<MethodDescription> getExtraMethodElementMatcher() {
		return isAnnotatedWith(Metered.class);
	}

	@Advice.OnMethodEnter
	public static void meter(@MeteredSignature String signature) {
		Stagemonitor.getMetric2Registry().meter(metricNameTemplate.build(signature)).mark();
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

		@Override
		protected NamingParameters getNamingParameters(MethodDescription instrumentedMethod) {
			final Metered metered = instrumentedMethod.getDeclaredAnnotations().ofType(Metered.class).loadSilent();
			return new NamingParameters(metered.name(), metered.absolute());
		}

		@Override
		public Class<MeteredSignature> getAnnotationClass() {
			return MeteredSignature.class;
		}
	}
}
