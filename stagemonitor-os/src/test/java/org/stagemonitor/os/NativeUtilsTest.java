package org.stagemonitor.os;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

public class NativeUtilsTest {
	@Test
	public void testGetLibraryPath() throws Exception {
		final String libraryPath = NativeUtils.addResourcesToLibraryPath(Arrays.asList("/sigar/libsigar-x86-linux.so"), "sigar-test");
		Assert.assertTrue(new File(System.getProperty("java.io.tmpdir")+"/sigar-test/libsigar-x86-linux.so").exists());
		Assert.assertTrue(libraryPath, libraryPath.endsWith("sigar-test"));
	}
}
