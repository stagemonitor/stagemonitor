package org.stagemonitor.configuration.converter;

public class LongValueConverter extends AbstractValueConverter<Long> {

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
