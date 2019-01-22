package org.stagemonitor.core.instrument;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.aspects.SignatureUtils;

public final class CallerUtil {

//	private static final Object javaLangAccessObject;
//
//	static {
//		if (ClassUtils.hasMethod("sun.misc.JavaLangAccess", "getStackTraceDepth", Throwable.class)) {
//			javaLangAccessObject = SharedSecrets.getJavaLangAccess();
//		} else {
//			javaLangAccessObject = null;
//		}
//	}

	private CallerUtil() {
	}

	/**
	 * Returns the signature of the method inside the monitored codebase which was last executed.
	 */
	public static String getCallerSignature() {
		if (Stagemonitor.getPlugin(CorePlugin.class).getIncludePackages().isEmpty()) {
			return null;
		}
//		if (javaLangAccessObject != null) {
//			return getCallerSignatureSharedSecrets();
//		} else {
//			return getCallerSignatureGetStackTrace();
//		}

		return getCallerSignatureGetStackTrace();

	}

//	private static String getCallerSignatureSharedSecrets() {
//		String executedBy = null;
//		Exception exception = new Exception();
//		final JavaLangAccess javaLangAccess = (JavaLangAccess) javaLangAccessObject;
//		for (int i = 2; i < javaLangAccess.getStackTraceDepth(exception); i++) {
//			final StackTraceElement stackTraceElement = javaLangAccess.getStackTraceElement(exception, i);
//			if (StagemonitorClassNameMatcher.isIncluded(stackTraceElement.getClassName())) {
//				executedBy = SignatureUtils.getSignature(stackTraceElement.getClassName(), stackTraceElement.getMethodName());
//				break;
//			}
//		}
//		return executedBy;
//	}

	private static String getCallerSignatureGetStackTrace() {
		String executedBy = null;
		for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
			if (StagemonitorClassNameMatcher.isIncluded(stackTraceElement.getClassName())) {
				executedBy = SignatureUtils.getSignature(stackTraceElement.getClassName(), stackTraceElement.getMethodName());
				break;
			}
		}
		return executedBy;
	}
}
