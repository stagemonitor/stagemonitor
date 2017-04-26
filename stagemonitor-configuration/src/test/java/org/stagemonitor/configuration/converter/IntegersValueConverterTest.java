package org.stagemonitor.configuration.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.stagemonitor.configuration.converter.SetValueConverter.immutableSet;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

public class IntegersValueConverterTest {

	private final ValueConverter<Collection<Integer>> converter = SetValueConverter.INTEGERS;

	@Test
	public void testConvertSingleValue() throws Exception {
		assertEquals(immutableSet(1), converter.convert("1"));
	}

	@Test
	public void testConvertMultipleValues() throws Exception {
		assertEquals(immutableSet(1, 2, 3, 4), converter.convert("1, 2,3  ,  4 "));
	}

	@Test
	public void testConvertNull() throws Exception {
		assertEquals(Collections.<Integer>emptySet(), converter.convert(null));
	}

	@Test
	public void testToString() throws Exception {
		assertEquals("1, 2, 3, 4", converter.toString(converter.convert("1, 2,3  ,  4 ")));
	}

	@Test
	public void testToStringNull() throws Exception {
		assertNull(converter.toString(null));
	}

	@Test
	public void testFail() throws Exception {
		try {
			converter.convert("a,2,c");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Can't convert 'a' to Integer.", e.getMessage());
		}
	}

}
