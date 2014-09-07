package org.stagemonitor.core.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class RegexListValueConverter implements ValueConverter<List<Pattern>> {

	private final StringsValueConverter stringsValueConverter = new StringsValueConverter();

	@Override
	public List<Pattern> convert(String s) {
		final Collection<String> strings = stringsValueConverter.convert(s);
		List<Pattern> result = new ArrayList<Pattern>(strings.size());
		for (String string : strings) {
			try {
				result.add(Pattern.compile(string));
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Could not compile pattern '" + s + "'", e);
			}
		}
		return result;
	}

	@Override
	public String toString(List<Pattern> value) {
		if (value == null) {
			return null;
		}
		final String s = new ArrayList<Pattern>(value).toString();
		return s.substring(1, s.length() - 1);
	}

}
