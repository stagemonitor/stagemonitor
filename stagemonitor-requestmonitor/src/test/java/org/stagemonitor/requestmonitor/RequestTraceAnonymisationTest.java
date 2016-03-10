package org.stagemonitor.requestmonitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

public class RequestTraceAnonymisationTest {

	private MeasurementSession measurementSession;
	private RequestMonitorPlugin requestMonitorPlugin;
	private RequestMonitor requestMonitor;

	@Before
	public void setUp() throws Exception {
		measurementSession = new MeasurementSession("RequestTraceAnonymisationTest", "test", "test");
		requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		final Configuration configuration = mock(Configuration.class);
		final CorePlugin corePlugin = mock(CorePlugin.class);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);
		when(requestMonitorPlugin.getDiscloseUsers()).thenReturn(Collections.emptySet());
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		requestMonitor = new RequestMonitor(configuration, mock(Metric2Registry.class), Collections.emptyList());
	}

	@Test
	public void testAnonymizeUserNameAndIp() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);

		final RequestTrace requestTrace = createRequestTrace();

		assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", requestTrace.getUsername());
		assertNull(requestTrace.getDisclosedUserName());
		assertEquals("123.123.123.0", requestTrace.getClientIp());
	}

	@Test
	public void testAnonymizeIp() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(false);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);

		final RequestTrace requestTrace = createRequestTrace();

		assertEquals("test", requestTrace.getUsername());
		assertNull(requestTrace.getDisclosedUserName());
		assertEquals("123.123.123.0", requestTrace.getClientIp());
	}

	@Test
	public void testDiscloseUserNameAndIp() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);
		when(requestMonitorPlugin.getDiscloseUsers()).thenReturn(Collections.singleton("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3"));

		final RequestTrace requestTrace = createRequestTrace();

		assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", requestTrace.getUsername());
		assertEquals("test", requestTrace.getDisclosedUserName());
		assertEquals("123.123.123.123", requestTrace.getClientIp());
	}

	@Test
	public void testDiscloseIp() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(false);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);
		when(requestMonitorPlugin.getDiscloseUsers()).thenReturn(Collections.singleton("test"));

		final RequestTrace requestTrace = createRequestTrace();

		assertEquals("test", requestTrace.getUsername());
		assertEquals("test", requestTrace.getDisclosedUserName());
		assertEquals("123.123.123.123", requestTrace.getClientIp());
	}

	@Test
	public void testNull() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);

		final RequestTrace requestTrace = createRequestTrace(null, null);

		assertNull(requestTrace.getUsername());
		assertNull(requestTrace.getDisclosedUserName());
		assertNull(requestTrace.getClientIp());
	}

	@Test
	public void testNullUser() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);

		final RequestTrace requestTrace = createRequestTrace(null, "123.123.123.123");

		assertNull(requestTrace.getUsername());
		assertNull(requestTrace.getDisclosedUserName());
		assertEquals("123.123.123.0", requestTrace.getClientIp());
	}

	@Test
	public void testNullIp() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(true);

		final RequestTrace requestTrace = createRequestTrace("test", null);

		assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", requestTrace.getUsername());
		assertNull(requestTrace.getDisclosedUserName());
		assertNull(requestTrace.getClientIp());
	}

	@Test
	public void testDontAnonymize() throws Exception {
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(false);
		when(requestMonitorPlugin.isAnonymizeIPs()).thenReturn(false);

		final RequestTrace requestTrace = createRequestTrace();

		assertEquals("test", requestTrace.getUsername());
		assertNull(requestTrace.getDisclosedUserName());
		assertEquals("123.123.123.123", requestTrace.getClientIp());
	}

	private RequestTrace createRequestTrace() {
		return createRequestTrace("test", "123.123.123.123");
	}

	private RequestTrace createRequestTrace(String username, String ip) {
		final RequestTrace requestTrace = new RequestTrace("1", measurementSession, requestMonitorPlugin);
		requestTrace.setUsername(username);
		requestTrace.setClientIp(ip);
		requestMonitor.anonymizeUserNameAndIp(requestTrace);
		return requestTrace;
	}

}