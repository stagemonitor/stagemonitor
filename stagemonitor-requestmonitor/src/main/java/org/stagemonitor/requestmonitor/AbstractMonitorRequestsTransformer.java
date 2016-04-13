package org.stagemonitor.requestmonitor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;

public class AbstractMonitorRequestsTransformer extends StagemonitorByteBuddyTransformer {

	private static final RequestMonitorPlugin configuration = Stagemonitor.getPlugin(RequestMonitorPlugin.class);

	@Override
	protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
		return AbstractMonitorRequestsTransformer.class;
	}

	@Advice.OnMethodEnter
	private static void monitorStart(@Advice.BoxedArguments Object[] args, @RequestName String requestName) {
		Stagemonitor.getPlugin(RequestMonitorPlugin.class).getRequestMonitor()
				.monitorStart(new MonitoredMethodRequest(requestName, null, args));
	}

	@Advice.OnMethodExit
	private static void monitorStop(@Advice.Thrown Throwable exception) {
		stop(exception);
	}

	public static void stop(Throwable exception) {
		final RequestMonitor requestMonitor = Stagemonitor.getPlugin(RequestMonitorPlugin.class).getRequestMonitor();
		if (exception != null && exception instanceof Exception) {
			requestMonitor.recordException((Exception) exception);
		}
		requestMonitor.monitorStop();
	}

	@Override
	protected List<StagemonitorDynamicValue<?>> getDynamicValues() {
		return Collections.<StagemonitorDynamicValue<?>>singletonList(new TimedSignatureDynamicValue());
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface RequestName {
	}

	public static class TimedSignatureDynamicValue extends StagemonitorDynamicValue<RequestName> {

		@Override
		public Class<RequestName> getAnnotationClass() {
			return RequestName.class;
		}

		@Override
		public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
							  ParameterDescription.InDefinedShape target,
							  AnnotationDescription.Loadable<RequestName> annotation,
							  boolean initialized) {
			return configuration.getBusinessTransactionNamingStrategy()
					.getBusinessTransationName(instrumentedMethod.getDeclaringType().getSimpleName(), instrumentedMethod.getName());
		}
	}

}
