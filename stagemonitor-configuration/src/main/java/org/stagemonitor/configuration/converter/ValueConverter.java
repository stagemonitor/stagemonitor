package org.stagemonitor.configuration.converter;

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

	/**
	 * Converts a value to a string, redacting all sensitive information specific to the value type.
	 *
	 * <p>For example, when the value type {@link T} is of type {@link java.net.URL} this method should redact the
	 * password in the {@link java.net.URL#userInfo}</p>
	 */
	String toSafeString(T value);
}
