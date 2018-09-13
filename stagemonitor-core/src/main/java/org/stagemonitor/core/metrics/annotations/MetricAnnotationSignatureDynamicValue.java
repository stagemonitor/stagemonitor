package org.stagemonitor.core.metrics.annotations;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import org.stagemonitor.core.metrics.aspects.SignatureUtils;

import java.lang.annotation.Annotation;

public abstract class MetricAnnotationSignatureDynamicValue<T extends Annotation> implements Advice.OffsetMapping.Factory<T> {

	public String getRequestName(MethodDescription instrumentedMethod) {
		final NamingParameters namingParameters = getNamingParameters(instrumentedMethod);
		return SignatureUtils.getSignature(instrumentedMethod.getDeclaringType().getTypeName(), instrumentedMethod.getName(),
				namingParameters.getNameFromAnnotation(), namingParameters.isAbsolute());
	}

	@Override
	public Advice.OffsetMapping make(ParameterDescription.InDefinedShape target, AnnotationDescription.Loadable<T> annotation, AdviceType adviceType) {
		return new Advice.OffsetMapping() {
			@Override
			public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Advice.ArgumentHandler argumentHandler, Sort sort) {
				return Advice.OffsetMapping.Target.ForStackManipulation.of(getRequestName(instrumentedMethod));
			}
		};
	}

	protected abstract NamingParameters getNamingParameters(MethodDescription instrumentedMethod);

	protected static class NamingParameters {
		private String nameFromAnnotation;
		private boolean absolute;

		public NamingParameters(String nameFromAnnotation, boolean absolute) {
			this.nameFromAnnotation = nameFromAnnotation;
			this.absolute = absolute;
		}

		public String getNameFromAnnotation() {
			return nameFromAnnotation;
		}

		public boolean isAbsolute() {
			return absolute;
		}
	}
}
