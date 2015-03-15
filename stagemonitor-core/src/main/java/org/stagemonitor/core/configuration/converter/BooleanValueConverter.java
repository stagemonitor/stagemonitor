package org.stagemonitor.core.configuration.converter;

public class BooleanValueConverter implements ValueConverter<Boolean> {

	public static final BooleanValueConverter INSTANCE = new BooleanValueConverter();

	private static final String TRUE = Boolean.TRUE.toString();
	private static final String FALSE = Boolean.FALSE.toString();

	@Override
	public Boolean convert(String s) {
		if (TRUE.equals(s) || FALSE.equals(s)) {
			return Boolean.valueOf(s);
		}
		throw new IllegalArgumentException("Can't convert '" + s + "' to Boolean.");
	}

	@Override
	public String toString(Boolean value) {
		if (value == null) {
			return null;
		}
		return String.valueOf(value);
	}
}
