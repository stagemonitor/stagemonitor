package org.stagemonitor.core.metrics.aspects;

import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.core.util.StringUtils;

public final class SignatureUtils {

	private SignatureUtils() {
	}

	public static String getSignature(String fullClassName, String methodName, String nameFromAnnotation, boolean absolute) {
		String result = StringUtils.isEmpty(nameFromAnnotation) ? methodName : nameFromAnnotation;

		if (!absolute) {
			result = ClassUtils.shorten(fullClassName) + "#" + result;
		}
		return result;
	}

}
