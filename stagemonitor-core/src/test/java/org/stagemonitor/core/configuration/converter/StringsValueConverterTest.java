package org.stagemonitor.core.configuration.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

public class StringsValueConverterTest {

	private final SetValueConverter<String> converter = SetValueConverter.STRINGS_VALUE_CONVERTER;
	private final SetValueConverter<String> lowerConverter = SetValueConverter.LOWER_STRINGS_VALUE_CONVERTER;

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
