package org.stagemonitor.core.converter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RegexMapValueConverterTest {

	private final RegexMapValueConverter converter = new RegexMapValueConverter();

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
