package org.stagemonitor.configuration.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;

public class ListValueConverter<T> extends AbstractCollectionValueConverter<List<T>, T> {

	public static final ListValueConverter<String> STRINGS_VALUE_CONVERTER =
			new ListValueConverter<String>(StringValueConverter.INSTANCE);

	public static final ListValueConverter<String> LOWER_STRINGS_VALUE_CONVERTER =
			new ListValueConverter<String>(StringValueConverter.LOWER_CASE);

	public static final ValueConverter<List<Integer>> INTEGERS =
			new ListValueConverter<Integer>(IntegerValueConverter.INSTANCE);


	public ListValueConverter(ValueConverter<T> valueConverter) {
		super(valueConverter);
	}

	public ListValueConverter(ValueConverter<T> valueConverter, String delimiter) {
		super(valueConverter, delimiter);
	}

	@Override
	public List<T> convert(String s) {
		if (s != null && s.length() > 0) {
			final ArrayList<T> result = new ArrayList<T>();
			for (String split : s.split(delimiter)) {
				result.add(valueConverter.convert(split.trim()));
			}
			return Collections.unmodifiableList(result);
		}
		return emptyList();
	}


}

