package org.stagemonitor.requestmonitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.MeasurementSession;

public class RequestTraceAnonymisationTest {

	private MeasurementSession measurementSession;
	private RequestMonitorPlugin requestMonitorPlugin;

	@Before
	public void setUp() throws Exception {
		measurementSession = new MeasurementSession("RequestTraceAnonymisationTest", "test", "test");
		requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		when(requestMonitorPlugin.isAnonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);
	}

	@Test
	public void testAnonymizeUserNameAndIp() throws Exception {
		when(requestMonitorPlugin.isAnonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);

		final RequestTrace requestTrace = createRequestTrace();

		assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", requestTrace.getUsername());
		assertEquals("123.123.123.0", requestTrace.getClientIp());
	}

	@Test
	public void testAnonymizeIp() throws Exception {
		when(requestMonitorPlugin.isAnonymizeUserNames()).thenReturn(false);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);

		final RequestTrace requestTrace = createRequestTrace();

		assertEquals("test", requestTrace.getUsername());
		assertEquals("123.123.123.0", requestTrace.getClientIp());
	}

	@Test
	public void testDiscloseUserNameAndIp() throws Exception {
		when(requestMonitorPlugin.isAnonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);
		when(requestMonitorPlugin.getDiscloseUsers()).thenReturn(Collections.singleton("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3"));

		final RequestTrace requestTrace = createRequestTrace();

		assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", requestTrace.getUsername());
		assertEquals("test", requestTrace.getDisclosedUserName());
		assertEquals("123.123.123.123", requestTrace.getClientIp());
	}

	@Test
	public void testDiscloseIp() throws Exception {
		when(requestMonitorPlugin.isAnonymizeUserNames()).thenReturn(false);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);
		when(requestMonitorPlugin.getDiscloseUsers()).thenReturn(Collections.singleton("test"));

		final RequestTrace requestTrace = createRequestTrace();

		assertEquals("test", requestTrace.getUsername());
		assertEquals("123.123.123.123", requestTrace.getClientIp());
	}

	@Test
	public void testNull() throws Exception {
		when(requestMonitorPlugin.isAnonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);

		final RequestTrace requestTrace = new RequestTrace("1", measurementSession, requestMonitorPlugin);
		requestTrace.setAndAnonymizeUserName(null);
		requestTrace.setAndAnonymizeClientIp(null);

		assertNull(requestTrace.getUsername());
		assertNull(requestTrace.getClientIp());
	}

	@Test
	public void testDontAnonymize() throws Exception {
		when(requestMonitorPlugin.isAnonymizeUserNames()).thenReturn(false);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(false);

		final RequestTrace requestTrace = createRequestTrace();

		assertEquals("test", requestTrace.getUsername());
		assertEquals("123.123.123.123", requestTrace.getClientIp());
	}

	private RequestTrace createRequestTrace() {
		final RequestTrace requestTrace = new RequestTrace("1", measurementSession, requestMonitorPlugin);
		requestTrace.setAndAnonymizeUserName("test");
		requestTrace.setAndAnonymizeClientIp("123.123.123.123");
		return requestTrace;
	}

}