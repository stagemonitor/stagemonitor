package org.stagemonitor.tracing.profiler;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;

import static org.stagemonitor.core.instrument.StagemonitorClassNameMatcher.isInsideMonitoredProject;

public class ProfilingTransformer extends StagemonitorByteBuddyTransformer {

	@Override
	public ElementMatcher.Junction<TypeDescription> getExtraExcludeTypeMatcher() {
		return makeSureClassesAreNotProfiledTwice();
	}

	/*
	 * If this is a subclass of ProfilingTransformer, make sure to not instrument classes
	 * which are matched by ProfilingTransformer
	 */
	private ElementMatcher.Junction<TypeDescription> makeSureClassesAreNotProfiledTwice() {
		return isSubclass() ? isInsideMonitoredProject() : ElementMatchers.<TypeDescription>none();
	}

	private boolean isSubclass() {
		return getClass() != ProfilingTransformer.class;
	}

	@Override
	protected Class<? extends StagemonitorByteBuddyTransformer> getAdviceClass() {
		return ProfilingTransformer.class;
	}

	@Advice.OnMethodEnter
	public static void enter(@ProfilerSignature String signature) {
		Profiler.start(signature);
	}

	@Advice.OnMethodExit(onThrowable = Throwable.class)
	public static void exit() {
		Profiler.stop();
	}

	@Override
	protected int getOrder() {
		return Integer.MAX_VALUE;
	}

	@Override
	protected List<Advice.OffsetMapping.Factory<? extends Annotation>> getOffsetMappingFactories() {
		return Collections.<Advice.OffsetMapping.Factory<?>>singletonList(new ProfilerDynamicValue());
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface ProfilerSignature {
	}

	public static class ProfilerDynamicValue implements Advice.OffsetMapping.Factory<ProfilerSignature> {

		@Override
		public Class<ProfilerSignature> getAnnotationType() {
			return ProfilerSignature.class;
		}

		@Override
		public Advice.OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<ProfilerSignature> annotation, AdviceType adviceType) {
			return new Advice.OffsetMapping() {
				@Override
				public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Advice.ArgumentHandler argumentHandler, Sort sort) {
					final String returnType = instrumentedMethod.getReturnType().asErasure().getSimpleName();
					final String className = instrumentedMethod.getDeclaringType().getTypeName();
					final String signature = String.format("%s %s.%s(%s)", returnType, className, instrumentedMethod.getName(), getSignature(instrumentedMethod));
					return Target.ForStackManipulation.of(signature);
				}
			};
		}

		public String getSignature(MethodDescription instrumentedMethod) {
			StringBuilder stringBuilder = new StringBuilder();
			boolean comma = false;
			for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList().asErasures()) {
				if (comma) {
					stringBuilder.append(',');
				} else {
					comma = true;
				}
				stringBuilder.append(typeDescription.getSimpleName());
			}
			return stringBuilder.toString();
		}
	}
}
