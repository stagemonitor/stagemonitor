package org.stagemonitor.core.metrics.aspects;

import javassist.CtMethod;
import org.stagemonitor.core.util.StringUtils;

public final class SignatureUtils {

	private SignatureUtils() {
	}

	public static String getSignature(CtMethod method, String nameFromAnnotation, boolean absolute) {
		String className = null;
		if (!absolute) {
			className = method.getDeclaringClass().getSimpleName();
		}
		return getSignature(className, method.getName(), nameFromAnnotation, absolute);
	}

	public static String getSignature(String simpleClassName, String methodName, String nameFromAnnotation, boolean absolute) {
		String result = StringUtils.isEmpty(nameFromAnnotation) ? methodName : nameFromAnnotation;

		if (!absolute) {
			result = simpleClassName + "#" + result;
		}
		return result;
	}

}
