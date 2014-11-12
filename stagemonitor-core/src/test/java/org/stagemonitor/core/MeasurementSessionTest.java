package org.stagemonitor.core;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.stagemonitor.junit.ConditionalTravisTestRunner;
import org.stagemonitor.junit.ExcludeOnTravis;

import static org.junit.Assert.assertNotNull;

@RunWith(ConditionalTravisTestRunner.class)
public class MeasurementSessionTest {

	@Test
	@ExcludeOnTravis
	public void testGetHostname() {
		assertNotNull(MeasurementSession.getNameOfLocalHost());
		assertNotNull(MeasurementSession.getHostNameFromEnv());
	}
}
