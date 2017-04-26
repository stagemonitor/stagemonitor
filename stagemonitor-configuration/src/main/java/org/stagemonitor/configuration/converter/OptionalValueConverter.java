package org.stagemonitor.configuration.converter;

import java.util.Optional;

public class OptionalValueConverter<T> implements ValueConverter<Optional<T>> {

	private final ValueConverter<T> valueConverter;

	public OptionalValueConverter(ValueConverter<T> valueConverter) {
		this.valueConverter = valueConverter;
	}

	@Override
	public Optional<T> convert(String s) throws IllegalArgumentException {
		return Optional.ofNullable(valueConverter.convert(s));
	}

	@Override
	public String toString(Optional<T> value) {
		return valueConverter.toString(value.orElse(null));
	}
}
