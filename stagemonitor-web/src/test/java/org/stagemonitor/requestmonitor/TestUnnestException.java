package org.stagemonitor.requestmonitor;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;
import org.springframework.web.util.NestedServletException;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.web.monitor.HttpRequestTrace;

public class TestUnnestException {

	@Test
	public void testUnnestNestedServletException() throws Exception {
		final MeasurementSession measurementSession = new MeasurementSession("TestUnnestException", "test", "test");
		final HttpRequestTrace requestTrace = new HttpRequestTrace("1", "/test", Collections.emptyMap(), "PROST",
				null, false, measurementSession, new RequestMonitorPlugin());

		requestTrace.setException(new NestedServletException("Eat this!", new RuntimeException("bazinga!")));

		assertEquals("java.lang.RuntimeException", requestTrace.getExceptionClass());
		assertEquals("bazinga!", requestTrace.getExceptionMessage());
	}

}
