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

	public static boolean isNotPresent(String className) {
		return !isPresent(className);
	}

	public static boolean isPresent(String className) {
		return forNameOrNull(className) != null;
	}

	public static boolean hasMethod(String className, String methodName, Class<?>... parameterTypes) {
		try {
			Class.forName(className).getMethod(methodName, parameterTypes);
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		} catch (ClassNotFoundException e) {
			return false;
		}
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

	/**
	 * Returns the same {@link String} like {@link Object#toString()} would.
	 * <p/>
	 * This {@link String} is less likely to have collisions as opposed to just using the
	 * {@link System#identityHashCode(Object)} of the class loader, because the {@link Class#getName()} is also included.
	 * <p/>
	 * Note that the returned {@link String} is not guaranteed to be collision free. That means that for two different
	 * {@link Object}s of the same class, there could still be collisions. To minimize the risk of a collision, the
	 * output of this method should not be used if a lots of unique keys are needed.
	 *
	 * @param obj the {@link Object} (must not be null)
	 * @return a {@link String} which can be used as the cache key for the provided {@link ClassLoader}
	 * @throws IllegalArgumentException if obj is null
	 */
	public static String getIdentityString(Object obj) {
		if (obj == null) {
			throw new IllegalArgumentException("obj must not be null");
		}
		return obj.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(obj));
	}

	public static String shorten(String fullClassName) {
		return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
	}
}
