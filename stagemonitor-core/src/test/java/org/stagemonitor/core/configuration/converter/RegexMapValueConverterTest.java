package org.stagemonitor.core.configuration.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.regex.Pattern;

import org.junit.Test;

public class RegexMapValueConverterTest {

	private final MapValueConverter<Pattern, String> converter = new MapValueConverter<Pattern, String>(RegexValueConverter.INSTANCE, StringValueConverter.INSTANCE);

	@Test
	public void testRoundtrip() {
		final String patterns = "(.*).js$: *.js,\n" +
				"(.*).css$: *.css,\n" +
				"(.*).jpg$: *.jpg,\n" +
				"(.*).jpeg$: *.jpeg,\n" +
				"(.*).png$: *.png";
		assertEquals(patterns, converter.toString(converter.convert(patterns)));
	}

	@Test
	public void testToStringNull() throws Exception {
		assertNull(converter.toString(null));
	}
}
