package org.stagemonitor.configuration.converter;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexValueConverter extends AbstractValueConverter<Pattern> {

	public static final RegexValueConverter INSTANCE = new RegexValueConverter();

	private RegexValueConverter() {
	}

	@Override
	public Pattern convert(String s) throws IllegalArgumentException {
		try {
			return Pattern.compile(s);
		} catch (PatternSyntaxException e) {
			throw new IllegalArgumentException("Error while trying to parse regex '" + s + "'");
		}
	}

	@Override
	public String toString(Pattern value) {
		return value.toString();
	}

}
