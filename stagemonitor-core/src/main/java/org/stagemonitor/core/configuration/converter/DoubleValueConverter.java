package org.stagemonitor.core.configuration.converter;

public class DoubleValueConverter implements ValueConverter<Double> {

	@Override
	public Double convert(String s) {
		try {
			return Double.valueOf(s.replace(',', '.'));
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Can't convert '" + s + "' to Double.", e);
		}
	}

	@Override
	public String toString(Double value) {
		if (value == null) {
			return null;
		}
		return String.valueOf(value);
	}
}
