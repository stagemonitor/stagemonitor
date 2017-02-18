package org.stagemonitor.requestmonitor.sampling;

import com.codahale.metrics.Meter;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RateLimitingPreExecutionInterceptorTest {

	private RateLimitingPreExecutionInterceptor interceptor;
	private RequestMonitorPlugin requestMonitorPlugin;
	private PreExecutionInterceptorContext context;
	private RequestMonitor.RequestInformation requestInformation;
	private Meter internalRequestReportingRate;
	private Meter externalRequestReportingRate;

	@Before
	public void setUp() throws Exception {
		interceptor = new RateLimitingPreExecutionInterceptor();
		final Configuration configuration = mock(Configuration.class);
		requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		requestInformation = mock(RequestMonitor.RequestInformation.class);
		when(requestInformation.isExternalRequest()).thenReturn(false);

		internalRequestReportingRate = mock(Meter.class);
		externalRequestReportingRate = mock(Meter.class);
		context = new PreExecutionInterceptorContext(configuration, requestInformation, internalRequestReportingRate,
				externalRequestReportingRate, mock(Metric2Registry.class));
		when(requestMonitorPlugin.getOnlyReportNSpansPerMinute()).thenReturn(0d);
	}

	@Test
	public void testNeverReportServerSpan() throws Exception {
		when(requestInformation.isExternalRequest()).thenReturn(false);
		when(requestInformation.isServerRequest()).thenReturn(true);
		when(internalRequestReportingRate.getOneMinuteRate()).thenReturn(0.0);
		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

	@Test
	public void testAlwaysReportServerSpan() throws Exception {
		when(requestInformation.isExternalRequest()).thenReturn(false);
		when(requestInformation.isServerRequest()).thenReturn(true);
		when(internalRequestReportingRate.getOneMinuteRate()).thenReturn(10_000_000.0 / 60);
		when(requestMonitorPlugin.getOnlyReportNSpansPerMinute()).thenReturn(1_000_000.0);

		interceptor.interceptReport(context);

		assertTrue(context.isReport());
	}

	@Test
	public void testRateNotExceededServerSpan() throws Exception {
		when(requestInformation.isExternalRequest()).thenReturn(false);
		when(requestInformation.isServerRequest()).thenReturn(true);
		when(internalRequestReportingRate.getOneMinuteRate()).thenReturn(10.0 / 60);
		when(requestMonitorPlugin.getOnlyReportNSpansPerMinute()).thenReturn(10.0);

		interceptor.interceptReport(context);

		assertTrue(context.isReport());
	}

	@Test
	public void testRateExceededServerSpan() throws Exception {
		when(requestInformation.isExternalRequest()).thenReturn(false);
		when(requestInformation.isServerRequest()).thenReturn(true);
		when(internalRequestReportingRate.getOneMinuteRate()).thenReturn(10.1 / 60);
		when(requestMonitorPlugin.getOnlyReportNSpansPerMinute()).thenReturn(10.0);

		interceptor.interceptReport(context);

		assertFalse(context.isReport());
	}

	@Test
	public void testNeverReportExternalRequest() throws Exception {
		when(requestInformation.isExternalRequest()).thenReturn(true);
		when(requestInformation.isServerRequest()).thenReturn(false);
		when(externalRequestReportingRate.getOneMinuteRate()).thenReturn(0.0);
		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

	@Test
	public void testAlwaysReportExternalRequest() throws Exception {
		when(requestInformation.isExternalRequest()).thenReturn(true);
		when(requestInformation.isServerRequest()).thenReturn(false);
		when(externalRequestReportingRate.getOneMinuteRate()).thenReturn(10_000_000.0 / 60);
		when(requestMonitorPlugin.getOnlyReportNExternalRequestsPerMinute()).thenReturn(1_000_000.0);

		interceptor.interceptReport(context);

		assertTrue(context.isReport());
	}

	@Test
	public void testRateNotExceededExternalRequest() throws Exception {
		when(requestInformation.isExternalRequest()).thenReturn(true);
		when(requestInformation.isServerRequest()).thenReturn(false);
		when(externalRequestReportingRate.getOneMinuteRate()).thenReturn(10.0 / 60);
		when(requestMonitorPlugin.getOnlyReportNExternalRequestsPerMinute()).thenReturn(10.0);

		interceptor.interceptReport(context);

		assertTrue(context.isReport());
	}

	@Test
	public void testRateExceededExternalRequest() throws Exception {
		when(requestInformation.isExternalRequest()).thenReturn(true);
		when(requestInformation.isServerRequest()).thenReturn(false);
		when(externalRequestReportingRate.getOneMinuteRate()).thenReturn(10.1 / 60);
		when(requestMonitorPlugin.getOnlyReportNExternalRequestsPerMinute()).thenReturn(10.0);

		interceptor.interceptReport(context);

		assertFalse(context.isReport());
	}
}
