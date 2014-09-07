package org.stagemonitor.core.configuration.converter;

public interface ValueConverter<T> {

	/**
	 * Converts a String into a specific type
	 *
	 * @param s the String to convert. Never null and always trimed
	 * @return the converted String
	 * @throws IllegalArgumentException if there was a error converting the String
	 */
	T convert(String s) throws IllegalArgumentException;

	/**
	 * Converts a value back to its string representation.
	 *
	 * @param value a configuration value
	 * @return the configuration value in its string representation
	 */
	String toString(T value);
}
