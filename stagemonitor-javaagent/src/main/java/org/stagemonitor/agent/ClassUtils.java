package org.stagemonitor.agent;

public class ClassUtils {

	public static Class<?> forNameOrNull(String className) {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
}
