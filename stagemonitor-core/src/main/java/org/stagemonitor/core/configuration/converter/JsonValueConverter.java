package org.stagemonitor.core.configuration.converter;

import com.fasterxml.jackson.core.type.TypeReference;

import org.stagemonitor.core.util.JsonUtils;

import java.io.IOException;

public class JsonValueConverter<T> implements ValueConverter<T> {

	private final TypeReference<T> typeReference;

	public JsonValueConverter(TypeReference<T> typeReference) {
		this.typeReference = typeReference;
	}

	@Override
	public T convert(String s) {
		try {
			return JsonUtils.getMapper().readValue(s, typeReference);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public String toString(T value) {
		return JsonUtils.toJson(value);
	}
}
