package org.stagemonitor.core.util;

import org.junit.Assert;
import org.junit.Test;

public class StringUtilsTest {

	@Test
	public void testRemoveStart() {
		Assert.assertEquals(StringUtils.removeStart("teststring", "test"), "string");
		Assert.assertEquals(StringUtils.removeStart("string", "test"), "string");
		Assert.assertEquals(StringUtils.removeStart("stringtest", "test"), "stringtest");
	}

}
