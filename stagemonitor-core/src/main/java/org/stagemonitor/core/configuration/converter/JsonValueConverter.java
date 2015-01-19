package org.stagemonitor.core.configuration.converter;

import org.stagemonitor.core.util.JsonUtils;

import java.io.IOException;

public class JsonValueConverter<T> implements ValueConverter<T> {

	private final Class<T> clazz;

	public JsonValueConverter(Class<T> clazz) {
		this.clazz = clazz;
	}

	@Override
	public T convert(String s) {
		try {
			return JsonUtils.getMapper().readValue(s, clazz);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString(T value) {
		return JsonUtils.toJson(value);
	}
}
