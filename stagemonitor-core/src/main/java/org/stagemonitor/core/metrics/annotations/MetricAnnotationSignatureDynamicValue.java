package org.stagemonitor.core.metrics.annotations;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.core.metrics.aspects.SignatureUtils;

import java.lang.annotation.Annotation;

public abstract class MetricAnnotationSignatureDynamicValue<T extends Annotation> extends StagemonitorByteBuddyTransformer.StagemonitorDynamicValue<T> {

	@Override
	protected Object doResolve(TypeDescription instrumentedType,
							   MethodDescription instrumentedMethod,
							   ParameterDescription.InDefinedShape target,
							   AnnotationDescription.Loadable<T> annotation, Assigner assigner, boolean initialized) {
		return getRequestName(instrumentedMethod);
	}

	public String getRequestName(MethodDescription instrumentedMethod) {
		final NamingParameters namingParameters = getNamingParameters(instrumentedMethod);
		return SignatureUtils.getSignature(instrumentedMethod.getDeclaringType().getTypeName(), instrumentedMethod.getName(),
				namingParameters.getNameFromAnnotation(), namingParameters.isAbsolute());
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
