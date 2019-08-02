package org.stagemonitor.core.instrument;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.aspects.SignatureUtils;

public final class CallerUtil {

	private CallerUtil() {
	}

	/**
	 * Returns the signature of the method inside the monitored codebase which was last executed.
	 */
	public static String getCallerSignature() {
		if (Stagemonitor.getPlugin(CorePlugin.class).getIncludePackages().isEmpty()) {
			return null;
		}

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
