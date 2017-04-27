package org.stagemonitor.tracing;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AbstractMonitorRequestsTransformer extends StagemonitorByteBuddyTransformer {

	@Override
	protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
		return AbstractMonitorRequestsTransformer.class;
	}

	@Advice.OnMethodEnter(inline = false)
	public static void monitorStart(@ParameterNames String parameterNames, @Advice.AllArguments Object[] args,
									@RequestName String requestName, @Advice.Origin("#t") String className,
									@Advice.Origin("#m") String methodName, @Advice.This(optional = true) Object thiz) {
		final String[] paramNames = parameterNames.split(",");
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		for (int i = 0; i < args.length; i++) {
			params.put(paramNames[i], args[i]);
		}

		final MonitoredMethodRequest monitoredRequest = new MonitoredMethodRequest(Stagemonitor.getConfiguration(), requestName, null, params);
		final TracingPlugin tracingPlugin = Stagemonitor.getPlugin(TracingPlugin.class);
		tracingPlugin.getRequestMonitor().monitorStart(monitoredRequest);
		if (requestName == null) {
			TracingPlugin
					.getCurrentSpan()
					.setOperationName(getBusinessTransationName(thiz != null ? thiz.getClass().getName() : className, methodName));
		}
	}

	@Advice.OnMethodExit(onThrowable = Throwable.class, inline = false)
	public static void monitorStop(@Advice.Thrown Throwable exception) {
		final RequestMonitor requestMonitor = Stagemonitor.getPlugin(TracingPlugin.class).getRequestMonitor();
		if (exception != null && exception instanceof Exception) {
			requestMonitor.recordException((Exception) exception);
		}
		requestMonitor.monitorStop();
	}

	@Override
	protected int getOrder() {
		return Integer.MIN_VALUE;
	}

	@Override
	protected List<StagemonitorDynamicValue<?>> getDynamicValues() {
		return Arrays.asList(new RequestNameDynamicValue(), new ParameterNamesDynamicValue());
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface RequestName {
	}

	public static class RequestNameDynamicValue extends StagemonitorDynamicValue<RequestName> {

		@Override
		public Class<RequestName> getAnnotationClass() {
			return RequestName.class;
		}

		@Override
		protected Object doResolve(TypeDescription instrumentedType,
								   MethodDescription instrumentedMethod,
								   ParameterDescription.InDefinedShape target,
								   AnnotationDescription.Loadable<RequestName> annotation,
								   Assigner assigner, boolean initialized) {
			return getRequestName(instrumentedMethod);
		}
	}

	public static String getRequestName(MethodDescription instrumentedMethod) {
		final AnnotationDescription.Loadable<MonitorRequests> monitorRequestsLoadable = instrumentedMethod
				.getDeclaredAnnotations()
				.ofType(MonitorRequests.class);
		if (monitorRequestsLoadable != null) {
			final MonitorRequests monitorRequests = monitorRequestsLoadable.loadSilent();
			if (!monitorRequests.requestName().isEmpty()) {
				return monitorRequests.requestName();
			}
			if (monitorRequests.resolveNameAtRuntime()) {
				return null;
			}
		}
		return getBusinessTransationName(instrumentedMethod.getDeclaringType().getTypeName(), instrumentedMethod.getName());
	}

	private static String getBusinessTransationName(String className, String methodName) {
		return configuration.getConfig(TracingPlugin.class)
				.getBusinessTransactionNamingStrategy()
				.getBusinessTransationName(className, methodName);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface ParameterNames {
	}

	public static class ParameterNamesDynamicValue extends StagemonitorDynamicValue<ParameterNames>{

		@Override
		public Class<ParameterNames> getAnnotationClass() {
			return ParameterNames.class;
		}

		@Override
		protected Object doResolve(TypeDescription instrumentedType,
								   MethodDescription instrumentedMethod,
								   ParameterDescription.InDefinedShape target,
								   AnnotationDescription.Loadable<ParameterNames> annotation,
								   Assigner assigner, boolean initialized) {
			StringBuilder params = new StringBuilder();
			for (ParameterDescription param : instrumentedMethod.getParameters()) {
				params.append(param.getName()).append(',');
			}
			return params.toString();
		}

	}

}
