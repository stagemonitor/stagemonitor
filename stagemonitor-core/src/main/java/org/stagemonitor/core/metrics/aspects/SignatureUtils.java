package org.stagemonitor.core.metrics.aspects;

import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.core.util.StringUtils;

public final class SignatureUtils {

	private SignatureUtils() {
	}

	public static String getSignature(String fullClassName, String methodName, String nameFromAnnotation, boolean absolute) {
		String name = StringUtils.isEmpty(nameFromAnnotation) ? methodName : nameFromAnnotation;
		if (absolute) {
			return name;
		}
		return getSignature(fullClassName, name);
	}

	public static String getSignature(String fullClassName, String methodName) {
		return ClassUtils.shorten(fullClassName) + "#" + methodName;
	}

}
