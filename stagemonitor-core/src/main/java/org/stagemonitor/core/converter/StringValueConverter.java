package org.stagemonitor.core.converter;

public class StringValueConverter implements ValueConverter<String> {

	@Override
	public String convert(String s) {
		return s;
	}

	@Override
	public String toString(String value) {
		return value;
	}
}
