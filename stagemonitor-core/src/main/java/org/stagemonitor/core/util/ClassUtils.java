package org.stagemonitor.core.util;

public class ClassUtils {

	private ClassUtils() {
	}

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
		return loadClassOrReturnNull(loader, className) != null;
	}

	public static Class<?> loadClassOrReturnNull(ClassLoader loader, String className) {
		if (loader == null) {
			loader = ClassLoader.getSystemClassLoader();
		}
		try {
			return loader.loadClass(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
}
