package org.stagemonitor.configuration.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

import org.junit.Test;

public class RegexListValueConverterTest {

	private final SetValueConverter<Pattern> converter =
		new SetValueConverter<Pattern>(RegexValueConverter.INSTANCE, ",(?![^()]*\\))");

	@Test
	public void testConvert() throws Exception {
		assertEquals(".*", converter.convert(".*").iterator().next().pattern());
	}

	@Test
	public void testConvertMultiple() throws Exception {
		Iterator<Pattern> results = converter.convert(".*,(.*,.*),^$").iterator();
		assertEquals(".*", results.next().pattern());
		assertEquals("(.*,.*)", results.next().pattern());
		assertEquals("^$", results.next().pattern());
		assertFalse(results.hasNext());
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
