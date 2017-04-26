package org.stagemonitor.configuration.converter;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BooleanValueConverterTest {

	private final BooleanValueConverter converter = new BooleanValueConverter();

	@Test
	public void testConvert() throws Exception {
		assertTrue(converter.convert("true"));
		assertFalse(converter.convert("false"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertNull() throws Exception {
		converter.convert(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertInvalidTrue() throws Exception {
		converter.convert("ture");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertInvalidFalse() throws Exception {
		converter.convert("fasle");
	}

	@Test
	public void testToStringNull() throws Exception {
		assertNull(converter.toString(null));
	}
}
