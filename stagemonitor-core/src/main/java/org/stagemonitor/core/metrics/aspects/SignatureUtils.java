package org.stagemonitor.core.metrics.aspects;

import org.aspectj.lang.Signature;

public final class SignatureUtils {

	public static String getSignature(Signature signature, String nameFromAnnotation, boolean absolute) {
		String className = null;
		if (!absolute) {
			className = signature.getDeclaringTypeName();
			className = className.substring(className.lastIndexOf('.') + 1, className.length());
		}
		return getSignature(className, signature.getName(), nameFromAnnotation, absolute);
	}

	public static String getSignature(String simpleClassName, String methodName, String nameFromAnnotation, boolean absolute) {
		String result = nameFromAnnotation.isEmpty() ? methodName : nameFromAnnotation;

		if (!absolute) {
			result = simpleClassName + "#" + result;
		}
		return result;
	}

}
