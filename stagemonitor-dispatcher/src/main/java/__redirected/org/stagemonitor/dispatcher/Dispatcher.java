package __redirected.org.stagemonitor.dispatcher;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This class is injected into the bootstrap classpath and is used to share objects between classloaders.
 * <p/>
 * The reason why this class is inside the __redirected package is because JBoss' ModuleClassLoader would otherwise not
 * load this class and throw a {@link ClassNotFoundException} when trying to access classes in normal packages from the bootstrap
 * classloader.
 * <p/>
 * See also https://github.com/jboss-modules/jboss-modules/blob/master/src/main/java/org/jboss/modules/Module.java#L92
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
	 * Gets a shared value by it's key
	 * <p/>
	 * Automatically casts the value to the desired type
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
	 * Gets a shared value by it's key
	 * <p/>
	 * Automatically casts the value to the desired type
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
	 * @return the underlying {@link ConcurrentMap}
	 */
	public static ConcurrentMap<String, Object> getValues() {
		return values;
	}
}
