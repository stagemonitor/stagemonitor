package org.stagemonitor.requestmonitor.tracing.jaeger;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.source.SimpleSource;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.sampling.PreExecutionInterceptorContext;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RateLimitingPreExecutionInterceptorTest {

	private RateLimitingPreExecutionInterceptor interceptor;
	private RequestMonitorPlugin requestMonitorPlugin;
	private PreExecutionInterceptorContext context;
	private RequestMonitor.RequestInformation requestInformation;

	@Before
	public void setUp() throws Exception {
		requestMonitorPlugin = new RequestMonitorPlugin();
		final Configuration configuration = new Configuration(Collections.singletonList(requestMonitorPlugin),
				Collections.singletonList(new SimpleSource()),
				null);

		requestInformation = mock(RequestMonitor.RequestInformation.class);
		when(requestInformation.isExternalRequest()).thenReturn(false);

		context = new PreExecutionInterceptorContext(configuration, requestInformation, mock(Metric2Registry.class));
		interceptor = new RateLimitingPreExecutionInterceptor();
		interceptor.init(configuration);
	}

	@Test
	public void testNeverReportServerSpan() throws Exception {
		requestMonitorPlugin.getRateLimitServerSpansPerMinuteOption().update(0d, SimpleSource.NAME);
		when(requestInformation.isExternalRequest()).thenReturn(false);
		when(requestInformation.isServerRequest()).thenReturn(true);
		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

	@Test
	public void testAlwaysReportServerSpan() throws Exception {
		when(requestInformation.isExternalRequest()).thenReturn(false);
		when(requestInformation.isServerRequest()).thenReturn(true);
		requestMonitorPlugin.getRateLimitServerSpansPerMinuteOption().update(1_000_000.0, SimpleSource.NAME);

		interceptor.interceptReport(context);

		assertTrue(context.isReport());
	}

	@Test
	public void testRateNotExceededThenExceededServerSpan() throws Exception {
		when(requestInformation.isExternalRequest()).thenReturn(false);
		when(requestInformation.isServerRequest()).thenReturn(true);
		requestMonitorPlugin.getRateLimitServerSpansPerMinuteOption().update(60d, SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertTrue(context.isReport());

		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

	@Test
	public void testNeverReportExternalRequest() throws Exception {
		when(requestInformation.isExternalRequest()).thenReturn(true);
		when(requestInformation.isServerRequest()).thenReturn(false);
		requestMonitorPlugin.getRateLimitClientSpansPerMinuteOption().update(0d, SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

	@Test
	public void testAlwaysReportExternalRequest() throws Exception {
		when(requestInformation.isExternalRequest()).thenReturn(true);
		when(requestInformation.isServerRequest()).thenReturn(false);
		requestMonitorPlugin.getRateLimitClientSpansPerMinuteOption().update(10_000_000.0, SimpleSource.NAME);

		interceptor.interceptReport(context);

		assertTrue(context.isReport());
	}

	@Test
	public void testRateExceededThenNotExceededExternalRequest() throws Exception {
		when(requestInformation.isExternalRequest()).thenReturn(true);
		when(requestInformation.isServerRequest()).thenReturn(false);
		requestMonitorPlugin.getRateLimitClientSpansPerMinuteOption().update(60.0, SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertTrue(context.isReport());

		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

}
