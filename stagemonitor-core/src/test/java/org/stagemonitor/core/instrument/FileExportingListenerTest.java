package org.stagemonitor.core.instrument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import com.codahale.metrics.annotation.Timed;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;

public class FileExportingListenerTest {

	@Before
	public void setUp() throws Exception {
		Stagemonitor.init();
	}

	@After
	public void tearDown() throws Exception {
		for (String exportedClass : FileExportingListener.exportedClasses) {
			new File(exportedClass).delete();
		}
	}

	@Test
	public void testExportIncludedClasses() throws Exception {
		new ExportMe();
		assertEquals(1, FileExportingListener.exportedClasses.size());
		assertTrue(FileExportingListener.exportedClasses.get(0).contains(ExportMe.class.getName()));
		final File exportedClass = new File(FileExportingListener.exportedClasses.get(0));
		assertTrue(exportedClass.exists());
	}

	@Test
	public void testDoNotExportNotIncludedClasses() throws Exception {
		new DoNotExportMe();
		assertEquals(0, FileExportingListener.exportedClasses.size());
	}

	private static class ExportMe {
		@Timed
		public void test() {
		}
	}
	private static class DoNotExportMe {}

}