package org.stagemonitor.core.converter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RegexListValueConverterTest {

	private final RegexListValueConverter converter = new RegexListValueConverter();

	@Test
	public void testConvert() throws Exception {
		assertEquals(".*", converter.convert(".*").get(0).pattern());
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
