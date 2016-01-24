package org.stagemonitor.core.configuration.converter;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.stagemonitor.core.util.StringUtils;

public class ListValueConverter<T> implements ValueConverter<List<T>> {

	public static final ListValueConverter<String> STRINGS_VALUE_CONVERTER =
			new ListValueConverter<String>(StringValueConverter.INSTANCE);

	public static final ListValueConverter<String> LOWER_STRINGS_VALUE_CONVERTER =
			new ListValueConverter<String>(StringValueConverter.LOWER_CASE);

	public static final ValueConverter<List<Integer>> INTEGERS =
			new ListValueConverter<Integer>(IntegerValueConverter.INSTANCE);

	private final ValueConverter<T> valueConverter;

	public ListValueConverter(ValueConverter<T> valueConverter) {
		this.valueConverter = valueConverter;
	}

	@Override
	public List<T> convert(String s) {
		if (s != null && s.length() > 0) {
			final ArrayList<T> result = new ArrayList<T>();
			for (String split : s.split(",")) {
				result.add(valueConverter.convert(split.trim()));
			}
			return Collections.unmodifiableList(result);
		}
		return emptyList();
	}

	@Override
	public String toString(List<T> value) {
		return StringUtils.asCsv(value);
	}

}

