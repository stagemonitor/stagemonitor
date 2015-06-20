package org.stagemonitor.core.configuration.converter;

/**
 * Converts an {@link Enum} to a {@link String} and back
 */
public class EnumValueConverter<T extends Enum<T>> implements ValueConverter<T> {

	private final Class<T> enumClass;

	public EnumValueConverter(Class<T> enumClass) {
		this.enumClass = enumClass;
	}

	/**
	 * Converts a String into an Enum.
	 *
	 * @param name The Enum's name. May be in the form THE_ENUM, the_enum or the-enum.
	 * @return The Enum
	 * @throws IllegalArgumentException if there is no such Enum constant
	 */
	@Override
	public T convert(String name) throws IllegalArgumentException {
		if (name == null) {
			throw new IllegalArgumentException("Cant convert 'null' to " + enumClass.getSimpleName());
		}
		try {
			return Enum.valueOf(enumClass, name);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			return Enum.valueOf(enumClass, name.toUpperCase());
		} catch (IllegalArgumentException e) {
			// ignore
		}
		try {
			return Enum.valueOf(enumClass, name.toUpperCase().replace("-", "_"));
		} catch (IllegalArgumentException e) {
			// ignore
		}
		throw new IllegalArgumentException("Can't convert " + name + " to " + enumClass.getSimpleName());

	}

	@Override
	public String toString(T value) {
		if (value == null) {
			return null;
		}
		return value.name();
	}
}
