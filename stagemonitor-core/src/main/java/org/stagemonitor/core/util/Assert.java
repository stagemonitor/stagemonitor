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

	/**
	 * Copy of {@code com.google.common.base.Preconditions#checkArgument}
	 */
	public static void checkArgument(boolean expression,
									 String errorMessageTemplate,
									 Object... errorMessageArgs) {
		if (!expression) {
			throw new IllegalArgumentException(String.format(errorMessageTemplate, errorMessageArgs));
		}
	}

	/**
	 * Copy of {@code com.google.common.base.Preconditions#checkArgument}
	 *
	 * Ensures the truth of an expression involving one or more parameters to the calling method.
	 *
	 * @param expression a boolean expression
	 * @throws IllegalArgumentException if {@code expression} is false
	 */
	public static void checkArgument(boolean expression) {
		if (!expression) {
			throw new IllegalArgumentException();
		}
	}

}
