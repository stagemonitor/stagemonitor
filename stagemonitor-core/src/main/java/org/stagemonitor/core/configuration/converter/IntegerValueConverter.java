package org.stagemonitor.core.configuration.converter;

public class IntegerValueConverter implements ValueConverter<Integer> {

	@Override
	public Integer convert(String s) {
		try {
			return Integer.valueOf(s);
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Can't '" + s + "' to Long.", e);
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
