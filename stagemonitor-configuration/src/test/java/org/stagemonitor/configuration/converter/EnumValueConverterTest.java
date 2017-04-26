package org.stagemonitor.configuration.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class EnumValueConverterTest {

	private static enum TestEnum {
		TEST_ENUM
	}

	private final EnumValueConverter<TestEnum> converter = new EnumValueConverter<TestEnum>(TestEnum.class);

	@Test
	public void testConvert() throws Exception {
		assertEquals(TestEnum.TEST_ENUM, converter.convert("TEST_ENUM"));
		assertEquals(TestEnum.TEST_ENUM, converter.convert("test_enum"));
		assertEquals(TestEnum.TEST_ENUM, converter.convert("test-enum"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertNull() throws Exception {
		converter.convert(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertInvalid() throws Exception {
		converter.convert("BEST");
	}

	@Test
	public void testToStringNull() throws Exception {
		assertNull(converter.toString(null));
	}
}
