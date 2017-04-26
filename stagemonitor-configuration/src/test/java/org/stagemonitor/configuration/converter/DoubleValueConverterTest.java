package org.stagemonitor.configuration.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DoubleValueConverterTest {

	private final ValueConverter<Double> converter = DoubleValueConverter.INSTANCE;

	@Test
	public void testConvert() throws Exception {
		assertEquals(3.1415, converter.convert(Double.toString(3.1415)), 0);
	}

	@Test
	public void testConvertDot() throws Exception {
		assertEquals(3.1415, converter.convert("3.1415"), 0);
	}

	@Test
	public void testConvertComma() throws Exception {
		assertEquals(3.1415, converter.convert("3,1415"), 0);
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
