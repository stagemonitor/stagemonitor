package org.stagemonitor.core.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
		final byte[] array = "test".getBytes();
		final ByteArrayInputStream bios = new ByteArrayInputStream(array);
		Assert.assertEquals("test", IOUtils.toString(bios));
	}

}
