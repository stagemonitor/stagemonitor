package org.stagemonitor.configuration.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.regex.Pattern;

import org.junit.Test;

public class RegexListValueConverterTest {

	private final SetValueConverter<Pattern> converter = new SetValueConverter<Pattern>(RegexValueConverter.INSTANCE);

	@Test
	public void testConvert() throws Exception {
		assertEquals(".*", converter.convert(".*").iterator().next().pattern());
	}

	@Test
	public void testToStringConvert() throws Exception {
		assertEquals(".*", converter.toString(converter.convert(".*")));
	}

	@Test
	public void testToStringNull() throws Exception {
		assertNull(converter.toString(null));
	}
}
