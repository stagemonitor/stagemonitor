package org.stagemonitor.core.util;

public class ClassUtils {

	public static Class<?> forNameOrNull(String className) {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public static boolean isPresent(String className) {
		return forNameOrNull(className) != null;
	}

	public static boolean canLoadClass(ClassLoader loader, String className) {
		if (loader == null) {
			loader = ClassLoader.getSystemClassLoader();
		}
		try {
			loader.loadClass(className);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
