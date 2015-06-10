package org.stagemonitor.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class IOUtilsTest {

	@Test
	public void testCopy() throws IOException {
		final byte[] array = new byte[] { 1, 2, 3 };
		final ByteArrayInputStream bios = new ByteArrayInputStream(array);
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IOUtils.copy(bios, baos);

		Assert.assertArrayEquals(array, baos.toByteArray());
	}

	@Test
	public void testWrite() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IOUtils.write("test", baos);

		Assert.assertEquals("test", baos.toString());
	}

	@Test
	public void testToString() throws IOException {
		final ByteArrayInputStream bios = new ByteArrayInputStream("test".getBytes());
		Assert.assertEquals("test", IOUtils.toString(bios));
	}

	@Test
	public void testGetResourceAsString() throws Exception {
		Assert.assertEquals("foo=bar", IOUtils.getResourceAsString("test.properties"));
	}

}
