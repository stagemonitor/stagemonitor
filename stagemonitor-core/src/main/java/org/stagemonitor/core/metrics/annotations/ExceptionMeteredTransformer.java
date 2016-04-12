package org.stagemonitor.core.metrics.annotations;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import com.codahale.metrics.annotation.ExceptionMetered;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;

public class ExceptionMeteredTransformer extends StagemonitorByteBuddyTransformer {

	@Override
	protected ElementMatcher.Junction<? super MethodDescription.InDefinedShape> getMethodElementMatcher() {
		return isAnnotatedWith(ExceptionMetered.class);
	}

	@Advice.OnMethodExit
	public static void meterException(@MetricsSignature String signature, @MeterExceptionsFor Class<? extends Exception> cause, @Advice.Thrown Throwable t) {
		if (t != null && cause.isInstance(t)) {
			Stagemonitor.getMetric2Registry().meter(name("exception_rate").tag("signature", signature).build()).mark();
		}
	}
}
