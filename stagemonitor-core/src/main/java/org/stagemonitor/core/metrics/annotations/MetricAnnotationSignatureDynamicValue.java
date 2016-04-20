package org.stagemonitor.core.metrics.annotations;

import java.lang.annotation.Annotation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.core.metrics.aspects.SignatureUtils;

public abstract class MetricAnnotationSignatureDynamicValue<T extends Annotation> extends StagemonitorByteBuddyTransformer.StagemonitorDynamicValue<T> {
	@Override
	public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
						  ParameterDescription.InDefinedShape target,
						  AnnotationDescription.Loadable<T> annotation,
						  boolean initialized) {
		final NamingParameters namingParameters = getNamingParameters(instrumentedMethod);
		return SignatureUtils.getSignature(instrumentedMethod.getDeclaringType().getSimpleName(), instrumentedMethod.getName(),
				namingParameters.getNameFromAnnotation(), namingParameters.isAbsolute());
	}

	protected abstract NamingParameters getNamingParameters(MethodDescription.InDefinedShape instrumentedMethod);

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
