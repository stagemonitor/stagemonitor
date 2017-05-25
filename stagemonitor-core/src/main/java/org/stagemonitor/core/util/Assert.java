package org.stagemonitor.core.util;

public final class Assert {

	private Assert() {
		throw new IllegalStateException();
	}

	public static void hasText(String subject, String message) {
		if (subject == null || subject.trim().isEmpty()) {
			throw new IllegalArgumentException(message);
		}
	}

	public static void notEmpty(String[] strings, String message) {
		if (strings.length == 0) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Copy of {@code com.google.common.base.Preconditions#checkArgument}.
	 */
	public static void checkArgument(boolean expression,
									 String errorMessageTemplate,
									 Object... errorMessageArgs) {
		if (!expression) {
			throw new IllegalArgumentException(String.format(errorMessageTemplate, errorMessageArgs));
		}
	}
}
