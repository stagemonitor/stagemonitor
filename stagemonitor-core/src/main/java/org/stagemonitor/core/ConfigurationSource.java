package org.stagemonitor.core;

public interface ConfigurationSource {

	/**
	 * Gets the value for a property key
	 *
	 * @param key the property key
	 * @return the value for the key or <code>null</code> if not found
	 */
	String getValue(String key);

	/**
	 * Reloads the configuration to pick up the latest changes
	 */
	void reload();
}
