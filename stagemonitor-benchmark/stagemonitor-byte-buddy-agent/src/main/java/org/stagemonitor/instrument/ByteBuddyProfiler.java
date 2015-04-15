package org.stagemonitor.instrument;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.Callable;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.MethodDelegation;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Origin;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.RuntimeType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.SuperCall;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.stagemonitor.requestmonitor.profiler.Profiler;

public class ByteBuddyProfiler {

	public static void premain(String agentArgs, Instrumentation inst) {
		new AgentBuilder.Default()
				.rebase(new ElementMatcher<TypeDescription>() {
					@Override
					public boolean matches(TypeDescription target) {
						return target.getCanonicalName().startsWith("org.stagemonitor.benchmark.profiler.ClassByteBuddyProfiled");
					}
				})
				.transform(new ProfilingTransformer())
				.installOn(inst);
	}

	private static class ProfilingTransformer implements AgentBuilder.Transformer {

		@Override
		public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription) {
			return builder
					.method(ElementMatchers.any())
					.intercept(MethodDelegation.to(ProfilingInterceptor.class));
		}
	}

	public static class ProfilingInterceptor {
		@RuntimeType
		public static Object profile(@Origin String signature, @SuperCall Callable<?> zuper) throws Exception {
			Profiler.start(signature);
			try {
				return zuper.call();
			} finally {
				Profiler.stop();
			}
		}
		/*
		@RuntimeType
		public static Object profile(@Origin(cacheMethod = true) Method method, @Origin Class clazz, @SuperCall Callable<?> zuper) throws Exception {
			String signature = getSignature(method, clazz);
			Profiler.start(signature);
			try {
				return zuper.call();
			} finally {
				Profiler.stop();
			}
		}*/
	}

}

