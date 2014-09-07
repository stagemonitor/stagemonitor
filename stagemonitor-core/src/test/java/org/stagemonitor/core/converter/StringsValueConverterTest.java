package org.stagemonitor.core.converter;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StringsValueConverterTest {

	private final StringsValueConverter converter = new StringsValueConverter();
	private final StringsValueConverter lowerConverter = new StringsValueConverter(true);

	@Test
	public void testConvertSingleValue() throws Exception {
		assertEquals(new HashSet<String>(Arrays.asList("a")), converter.convert("a"));
		assertEquals(new HashSet<String>(Arrays.asList("a")), lowerConverter.convert("A"));
	}

	@Test
	public void testConvertMultipleValues() throws Exception {
		assertEquals(new HashSet<String>(Arrays.asList("a", "b", "c", "d")), converter.convert("a, b,c  ,  d "));
		assertEquals(new HashSet<String>(Arrays.asList("a", "b", "c", "d")), lowerConverter.convert("A, b,C  ,  D "));
	}

	@Test
	public void testConvertNull() throws Exception {
		assertEquals(Collections.<String>emptySet(), converter.convert(null));
	}

	@Test
	public void testToString() throws Exception {
		assertEquals("a, b, c, d", converter.toString(converter.convert("a, b,c  ,  d ")));
	}

	@Test
	public void testToStringNull() throws Exception {
		assertNull(converter.toString(null));
	}
}
