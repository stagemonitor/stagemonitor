package org.stagemonitor.core;

import org.junit.Test;
import org.stagemonitor.junit.ExcludeOnTravis;

import static org.junit.Assert.assertNotNull;

public class MeasurementSessionTest {

	@Test
	@ExcludeOnTravis
	public void testGetHostname() {
		assertNotNull(MeasurementSession.getNameOfLocalHost());
		assertNotNull(MeasurementSession.getHostNameFromEnv());
	}
}
