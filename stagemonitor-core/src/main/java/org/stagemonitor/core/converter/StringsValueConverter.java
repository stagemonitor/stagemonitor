package org.stagemonitor.core.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

import static java.util.Collections.emptySet;

public class StringsValueConverter implements ValueConverter<Collection<String>> {

	private final boolean lowerCase;

	public StringsValueConverter() {
		this(false);
	}

	public StringsValueConverter(boolean lowerCase) {
		this.lowerCase = lowerCase;
	}

	@Override
	public Collection<String> convert(String s) {
		if (s != null && s.length() > 0) {
			final LinkedHashSet<String> result = new LinkedHashSet<String>();
			for (String split : s.split(",")) {
				result.add(lowerCase ? split.trim().toLowerCase() : split.trim());
			}
			return result;
		}
		return emptySet();
	}

	@Override
	public String toString(Collection<String> value) {
		if (value == null) {
			return null;
		}
		final String s = new ArrayList<String>(value).toString();
		return s.substring(1, s.length() - 1);
	}

}
