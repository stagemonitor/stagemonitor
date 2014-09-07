package org.stagemonitor.core.converter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LongValueConverterTest {

	private final LongValueConverter converter = new LongValueConverter();

	@Test
	public void testConvert() throws Exception {
		assertEquals(Long.valueOf(Long.MAX_VALUE), converter.convert(Long.toString(Long.MAX_VALUE)));
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
