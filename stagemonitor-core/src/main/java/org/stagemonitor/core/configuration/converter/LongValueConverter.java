package org.stagemonitor.core.configuration.converter;

public class LongValueConverter implements ValueConverter<Long> {

	public static final LongValueConverter INSTANCE = new LongValueConverter();

	@Override
	public Long convert(String s) {
		try {
			return Long.valueOf(s);
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Can't convert '" + s + "' to Long.", e);
		}
	}

	@Override
	public String toString(Long value) {
		if (value == null) {
			return null;
		}
		return String.valueOf(value);
	}
}
