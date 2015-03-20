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
}
