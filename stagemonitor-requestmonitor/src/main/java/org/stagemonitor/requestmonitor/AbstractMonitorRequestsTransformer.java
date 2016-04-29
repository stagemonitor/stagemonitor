package org.stagemonitor.requestmonitor;

import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.not;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;

public class AbstractMonitorRequestsTransformer extends StagemonitorByteBuddyTransformer {

	@Override
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getMethodElementMatcher() {
		// static methods currently can't be monitored as we are injecting the this reference in monitorStart
		// @Advice.This Object thiz
		// to be able to get the runtime classname
		return super.getMethodElementMatcher().and(not(isStatic()));
	}

	@Override
	protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
		return AbstractMonitorRequestsTransformer.class;
	}

	@Advice.OnMethodEnter(inline = false)
	public static void monitorStart(@ParameterNames String parameterNames, @Advice.BoxedArguments Object[] args,
									@RequestName String requestName, @Advice.Origin("#m") String methodName,
									@Advice.This Object thiz) {
		final String[] paramNames = parameterNames.split(",");
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		for (int i = 0; i < args.length; i++) {
			params.put(paramNames[i], args[i]);
		}

		final MonitoredMethodRequest monitoredRequest = new MonitoredMethodRequest(Stagemonitor.getConfiguration(), requestName, null, params);
		final RequestMonitorPlugin requestMonitorPlugin = Stagemonitor.getPlugin(RequestMonitorPlugin.class);
		requestMonitorPlugin.getRequestMonitor().monitorStart(monitoredRequest);
		final RequestTrace request = RequestMonitor.getRequest();
		if (requestName == null && request != null) {
			request.setName(requestMonitorPlugin
					.getBusinessTransactionNamingStrategy()
					.getBusinessTransationName(thiz.getClass().getName(), methodName));
		}
	}

	@Advice.OnMethodExit(onThrowable = Throwable.class, inline = false)
	public static void monitorStop(@Advice.Thrown Throwable exception) {
		final RequestMonitor requestMonitor = Stagemonitor.getPlugin(RequestMonitorPlugin.class).getRequestMonitor();
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
		public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
							  ParameterDescription.InDefinedShape target,
							  AnnotationDescription.Loadable<RequestName> annotation,
							  boolean initialized) {
			return getRequestName(instrumentedMethod);
		}
	}

	public static String getRequestName(MethodDescription.InDefinedShape instrumentedMethod) {
		final AnnotationDescription.Loadable<MonitorRequests> monitorRequestsLoadable = instrumentedMethod.getDeclaredAnnotations().ofType(MonitorRequests.class);
		if (monitorRequestsLoadable != null) {
			final MonitorRequests monitorRequests = monitorRequestsLoadable.loadSilent();
			if (!monitorRequests.requestName().isEmpty()) {
				return monitorRequests.requestName();
			}
			if (monitorRequests.resolveNameAtRuntime()) {
				return null;
			}
		}
		final String typeName = instrumentedMethod.getDeclaringType().getName();
		return configuration.getConfig(RequestMonitorPlugin.class).getBusinessTransactionNamingStrategy()
				.getBusinessTransationName(typeName, instrumentedMethod.getName());
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface ParameterNames {
	}

	public static class ParameterNamesDynamicValue extends StagemonitorDynamicValue<ParameterNames> {

		@Override
		public Class<ParameterNames> getAnnotationClass() {
			return ParameterNames.class;
		}

		@Override
		public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
							  ParameterDescription.InDefinedShape target,
							  AnnotationDescription.Loadable<ParameterNames> annotation,
							  boolean initialized) {
			StringBuilder params = new StringBuilder();
			for (ParameterDescription.InDefinedShape param : instrumentedMethod.getParameters()) {
				params.append(param.getName()).append(',');
			}
			return params.toString();
		}
	}

}
