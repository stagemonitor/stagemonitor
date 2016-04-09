package org.stagemonitor.dispatcher;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This class is injected into the bootstrap classpath and is used to share objects between classloaders
 */
public class Dispatcher {

	private static ConcurrentMap<String, Object> values = new ConcurrentHashMap<String, Object>();

	/**
	 * Returns the underlying {@link ConcurrentMap}
	 * @return the underlying {@link ConcurrentMap}
	 */
	public static ConcurrentMap<String, Object> getValues() {
		return values;
	}
}
