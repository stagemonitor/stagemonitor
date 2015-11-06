package org.stagemonitor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.junit.ConditionalTravisTestRunner;
import org.stagemonitor.junit.ExcludeOnTravis;

@RunWith(ConditionalTravisTestRunner.class)
public class MeasurementSessionTest {

	@Test
	@ExcludeOnTravis
	public void testGetHostname() {
		assertNotNull(MeasurementSession.getNameOfLocalHost());
	}

	@Test
	@Ignore
	public void testGetHostnameFromEnv() {
		assertNotNull(MeasurementSession.getHostNameFromEnv());
	}

	@Test
	public void testToJson() throws Exception {
		MeasurementSession measurementSession = new MeasurementSession("app", "host", "instance");
		final MeasurementSession jsonSession = JsonUtils.getMapper().readValue(JsonUtils.toJson(measurementSession), MeasurementSession.class);
		assertEquals(measurementSession.getApplicationName(), jsonSession.getApplicationName());
		assertEquals(measurementSession.getHostName(), jsonSession.getHostName());
		assertEquals(measurementSession.getInstanceName(), jsonSession.getInstanceName());
		assertEquals(measurementSession.getInstanceName(), jsonSession.getInstanceName());
		assertEquals(measurementSession.getId(), jsonSession.getId());
		assertEquals(measurementSession.getStart(), jsonSession.getStart());
	}
}
