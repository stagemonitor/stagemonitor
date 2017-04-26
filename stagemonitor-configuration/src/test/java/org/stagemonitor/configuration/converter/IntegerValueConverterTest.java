package org.stagemonitor.configuration.converter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class IntegerValueConverterTest {

	private final IntegerValueConverter converter = new IntegerValueConverter();

	@Test
	public void testConvert() throws Exception {
		assertEquals(Integer.valueOf(Integer.MAX_VALUE), converter.convert(Integer.toString(Integer.MAX_VALUE)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertNull() throws Exception {
		converter.convert(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertInvalidTrue() throws Exception {
		converter.convert("one");
	}

	@Test
	public void testToStringNull() throws Exception {
		assertNull(converter.toString(null));
	}
}
