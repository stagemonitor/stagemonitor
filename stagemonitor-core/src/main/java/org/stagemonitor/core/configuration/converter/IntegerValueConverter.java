package org.stagemonitor.core.configuration.converter;

public class IntegerValueConverter implements ValueConverter<Integer> {

	public static final IntegerValueConverter INSTANCE = new IntegerValueConverter();

	@Override
	public Integer convert(String s) {
		try {
			return Integer.valueOf(s);
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Can't convert '" + s + "' to Integer.", e);
		}
	}

	@Override
	public String toString(Integer value) {
		if (value == null) {
			return null;
		}
		return String.valueOf(value);
	}
}
