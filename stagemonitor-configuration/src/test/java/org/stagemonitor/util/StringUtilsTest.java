package org.stagemonitor.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class StringUtilsTest {

	@Test
	public void testToCommaSeparatedString() {
		assertThat(StringUtils.toCommaSeparatedString("foo", "bar")).isEqualTo("foo,bar");
		assertThat(StringUtils.toCommaSeparatedString("foo")).isEqualTo("foo");
		assertThat(StringUtils.toCommaSeparatedString()).isEqualTo("");
	}

	@Test
	public void testRemoveStart() {
		assertEquals(StringUtils.removeStart("teststring", "test"), "string");
		assertEquals(StringUtils.removeStart("string", "test"), "string");
		assertEquals(StringUtils.removeStart("stringtest", "test"), "stringtest");
	}

	@Test
	public void testSha1Hash() throws Exception {
		// result from org.apache.commons.codec.digest.DigestUtils.sha1Hex
		assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", StringUtils.sha1Hash("test"));
	}
}
