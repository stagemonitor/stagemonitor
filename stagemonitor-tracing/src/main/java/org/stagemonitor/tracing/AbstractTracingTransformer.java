package org.stagemonitor.tracing;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.tracing.metrics.MetricsSpanEventListener;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.opentracing.Span;

public class AbstractTracingTransformer extends StagemonitorByteBuddyTransformer {

	@Override
	protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
		return AbstractTracingTransformer.class;
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
		final Span span = TracingPlugin.getCurrentSpan();
		if (requestName == null) {
			span.setOperationName(getBusinessTransationName(thiz != null ? thiz.getClass().getName() : className, methodName));
		}
		span.setTag(MetricsSpanEventListener.ENABLE_TRACKING_METRICS_TAG, true);
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
	protected List<Advice.OffsetMapping.Factory<? extends Annotation>> getOffsetMappingFactories() {
		final Advice.OffsetMapping.Factory<? extends Annotation> requestNameDynamicValue = new RequestNameDynamicValue();
		return Arrays.asList(requestNameDynamicValue, new ParameterNamesDynamicValue());
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface RequestName {
	}

	public static class RequestNameDynamicValue implements Advice.OffsetMapping.Factory<RequestName> {

		@Override
		public Class<RequestName> getAnnotationType() {
			return RequestName.class;
		}

		@Override
		public Advice.OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<RequestName> annotation, AdviceType adviceType) {
			return new Advice.OffsetMapping() {
				@Override
				public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context) {
					final String requestName = getRequestName(instrumentedMethod);
					if (requestName != null) {
						return Target.ForStackManipulation.of(requestName);
					} else {
						return new Target.ForStackManipulation(NullConstant.INSTANCE);
					}
				}
			};
		}
	}

	public static String getRequestName(MethodDescription instrumentedMethod) {
		final AnnotationDescription.Loadable<Traced> monitorRequestsLoadable = instrumentedMethod
				.getDeclaredAnnotations()
				.ofType(Traced.class);
		if (monitorRequestsLoadable != null) {
			final Traced traced = monitorRequestsLoadable.loadSilent();
			if (!traced.requestName().isEmpty()) {
				return traced.requestName();
			}
			if (traced.resolveNameAtRuntime()) {
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

	public static class ParameterNamesDynamicValue implements Advice.OffsetMapping.Factory<ParameterNames>{

		@Override
		public Class<ParameterNames> getAnnotationType() {
			return ParameterNames.class;
		}

		@Override
		public Advice.OffsetMapping make(ParameterDescription.InDefinedShape target,
										 AnnotationDescription.Loadable<ParameterNames> annotation,
										 AdviceType adviceType) {
			return new Advice.OffsetMapping() {
				@Override
				public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Context context) {
					final StringBuilder params = new StringBuilder();
					for (ParameterDescription param : instrumentedMethod.getParameters()) {
						params.append(param.getName()).append(',');
					}		return Advice.OffsetMapping.Target.ForStackManipulation.of(params.toString());
				}
			};
		}
	}

}
