package org.stagemonitor.core.configuration.source;

import java.io.IOException;

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
	void reload() throws IOException;

	/**
	 * Returns the name of the configuration source.
	 * For a properties file 'name.properties' could be returned.
	 *
	 * @return the name of the configuration source
	 */
	String getName();

	/**
	 * Returns whether or not it is possible to save values with the {@link #save(String, String, String)} method.
	 * <p/>
	 * If this method returns false, {@link #save(String, String, String)} must throw a {@link UnsupportedOperationException}
	 *
	 * @return <code>true</code>, if saving is possible, <code>false</code> otherwise
	 */
	boolean isSavingPossible();

	/**
	 *
	 *
	 * @return <code>true</code>, if saving to this configuration source is persistent, <false>otherwise</false>
	 */
	boolean isSavingPersistent();

	/**
	 * Directly saves the value to the configuration source without checking passwords
	 *
	 * @param key a existing config key
	 * @param value a valid value to save
	 * @throws IOException if there was an error saving the key to the configuration source
	 */
	void save(String key, String value) throws IOException;
}
