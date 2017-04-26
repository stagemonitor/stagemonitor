package org.stagemonitor.configuration.converter;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class MapValueConverter<K, V> implements ValueConverter<Map<K, V>> {

	private final ValueConverter<K> keyValueConverter;
	private final ValueConverter<V> valueValueConverter;
	private String valueSeparator;
	private String entrySeparator;

	public static final ValueConverter<Map<Pattern, String>> REGEX_MAP_VALUE_CONVERTER =
			new MapValueConverter<Pattern, String>(RegexValueConverter.INSTANCE, StringValueConverter.INSTANCE);


	public MapValueConverter(ValueConverter<K> keyValueConverter, ValueConverter<V> valueValueConverter) {
		this(keyValueConverter, valueValueConverter, ":", ",");
	}

	public MapValueConverter(ValueConverter<K> keyValueConverter, ValueConverter<V> valueValueConverter, String valueSeparator, String entrySeparator) {
		this.keyValueConverter = keyValueConverter;
		this.valueValueConverter = valueValueConverter;
		this.valueSeparator = valueSeparator;
		this.entrySeparator = entrySeparator;
	}

	@Override
	public Map<K, V> convert(String s) {
		if (s == null || s.trim().isEmpty()) {
			return Collections.emptyMap();
		}
		try {
			String[] groups = s.trim().split(entrySeparator);
			Map<K, V> map = new LinkedHashMap<K, V>(groups.length);

			for (String group : groups) {
				group = group.trim();
				String[] keyValue = group.split(valueSeparator);
				if (keyValue.length != 2) {
					throw new IllegalArgumentException();
				}
				map.put(keyValueConverter.convert(keyValue[0].trim()), valueValueConverter.convert(keyValue[1].trim()));
			}
			return Collections.unmodifiableMap(map);
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Error while parsing map. " +
					"Expected format <regex>: <name>[, <regex>: <name>]. Actual value: '" + s + ".'", e);
		}
	}

	@Override
	public String toString(Map<K, V> value) {
		if (value == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (Iterator<Map.Entry<K, V>> iterator = value.entrySet().iterator(); iterator.hasNext(); ) {
			Map.Entry<K, V> entry = iterator.next();
			sb.append(keyValueConverter.toString(entry.getKey())).append(valueSeparator).append(' ').append(valueValueConverter.toString(entry.getValue()));
			if (iterator.hasNext()) {
				sb.append(entrySeparator).append('\n');
			}
		}
		return sb.toString();
	}
}
