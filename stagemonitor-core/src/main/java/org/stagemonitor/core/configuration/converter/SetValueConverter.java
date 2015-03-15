package org.stagemonitor.core.configuration.converter;

import static java.util.Collections.emptySet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class SetValueConverter<T> implements ValueConverter<Collection<T>> {

	public static final SetValueConverter<String> STRINGS_VALUE_CONVERTER =
			new SetValueConverter<String>(StringValueConverter.INSTANCE);

	public static final SetValueConverter<String> LOWER_STRINGS_VALUE_CONVERTER =
			new SetValueConverter<String>(StringValueConverter.LOWER_CASE);

	public static final ValueConverter<Collection<Integer>> INTEGERS =
			new SetValueConverter<Integer>(IntegerValueConverter.INSTANCE);

	private final ValueConverter<T> valueConverter;

	public SetValueConverter(ValueConverter<T> valueConverter) {
		this.valueConverter = valueConverter;
	}

	@Override
	public Collection<T> convert(String s) {
		if (s != null && s.length() > 0) {
			final LinkedHashSet<T> result = new LinkedHashSet<T>();
			for (String split : s.split(",")) {
				result.add(valueConverter.convert(split.trim()));
			}
			return Collections.unmodifiableSet(result);
		}
		return emptySet();
	}

	@Override
	public String toString(Collection<T> value) {
		if (value == null) {
			return null;
		}
		final String s = new ArrayList<T>(value).toString();
		// removes []
		return s.substring(1, s.length() - 1);
	}

	public static <T> Set<T> immutableSet(T... values) {
		return Collections.unmodifiableSet(new LinkedHashSet<T>(Arrays.asList(values)));
	}
}

