package __redirected.org.stagemonitor.dispatcher;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This class is injected into the bootstrap classpath and is used to share objects between classloaders. <p> The reason
 * why this class is inside the __redirected package is because JBoss' ModuleClassLoader would otherwise not load this
 * class and throw a {@link ClassNotFoundException} when trying to access classes in normal packages from the bootstrap
 * classloader. <p> See also https://github.com/jboss-modules/jboss-modules/blob/master/src/main/java/org/jboss/modules/Module.java#L92
 */
public class Dispatcher {

	private static ConcurrentMap<String, Object> values = new ConcurrentHashMap<String, Object>();

	/**
	 * Add a value to the shared map
	 *
	 * @param key   the key that can be used to retrieve the value via {@link #get(String)}
	 * @param value the object to share
	 */
	public static void put(String key, Object value) {
		values.put(key, value);
	}

	/**
	 * Gets a shared value by it's key <p> Automatically casts the value to the desired type
	 *
	 * @param key the key
	 * @param <T> the type of the value
	 * @return the shared value
	 */
	@SuppressWarnings("unchecked")
	public static <T> T get(String key) {
		return (T) values.get(key);
	}


	/**
	 * Gets a shared value by it's key <p> Automatically casts the value to the desired type
	 *
	 * @param key        the key
	 * @param valueClass the class of the value
	 * @param <T>        the type of the value
	 * @return the shared value
	 */
	@SuppressWarnings("unchecked")
	public static <T> T get(String key, Class<T> valueClass) {
		return (T) values.get(key);
	}

	/**
	 * Gets a shared value by it's key
	 *
	 * @param key the key
	 * @return the shared value
	 */
	public static Object getObject(String key) {
		return values.get(key);
	}

	/**
	 * Returns the underlying {@link ConcurrentMap}
	 *
	 * @return the underlying {@link ConcurrentMap}
	 */
	public static ConcurrentMap<String, Object> getValues() {
		return values;
	}


	// TODO using old and new stagemonitor versions on the same application server
	// - undefined behaviour -> NoSuchMethodError
	// - compatibility
	//   - upwards and backwards
	//   - which Dispatcher is injected: latest init wins vs latest version wins
	//   -> more error-prone and higher maintenance
	// - not supported -> disable stagemonitor
	/**
	 * This utility method can be used to check whether a certain object can be loaded by the current thread's context
	 * class loader
	 *
	 * <p> In other words, it check if the following condition returns true: </p>
	 * <pre>{@code
	 * Thread.currentThread().getContextClassLoader().loadClass(o.getClass().getName()) == clazz
	 * }</pre>
	 *
	 * This can be useful to check if the provided object belongs to the current application
	 */
	public static boolean isVisibleToCurrentContextClassLoader(Object o) {
		return isVisibleTo(o, Thread.currentThread().getContextClassLoader());
	}

	public static boolean isVisibleTo(Object o, ClassLoader cl) {
		return o != null && isVisibleTo(o.getClass(), cl);
	}

	public static boolean isVisibleTo(Class<?> clazz, ClassLoader cl) {
		if (cl == null) {
			cl = ClassLoader.getSystemClassLoader();
		}
		try {
			return cl.loadClass(clazz.getName()) == clazz;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

}
