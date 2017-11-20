package org.stagemonitor.configuration.converter;

public class IntegerValueConverter extends AbstractValueConverter<Integer> {

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
