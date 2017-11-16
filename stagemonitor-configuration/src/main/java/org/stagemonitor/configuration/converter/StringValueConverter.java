package org.stagemonitor.configuration.converter;

public class StringValueConverter extends AbstractValueConverter<String> {

	public static final StringValueConverter INSTANCE = new StringValueConverter(false);

	public static final StringValueConverter LOWER_CASE = new StringValueConverter(true);

	private final boolean lowerCase;

	private StringValueConverter(boolean lowerCase) {
		this.lowerCase = lowerCase;
	}

	@Override
	public String convert(String s) {
		if (lowerCase && s != null) {
			return s.toLowerCase();
		}
		return s;
	}

	@Override
	public String toString(String value) {
		return value;
	}
}
