package org.stagemonitor.configuration.converter;

public abstract class AbstractValueConverter<T> implements ValueConverter<T> {

	@Override
	public String toSafeString(T value) {
		return toString(value);
	}

}
