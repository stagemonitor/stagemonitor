package org.stagemonitor.tracing.tracing.jaeger;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.source.SimpleSource;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.sampling.PreExecutionInterceptorContext;
import org.stagemonitor.tracing.sampling.RateLimitingPreExecutionInterceptor;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RateLimitingPreExecutionInterceptorTest {

	private RateLimitingPreExecutionInterceptor interceptor;
	private TracingPlugin tracingPlugin;
	private PreExecutionInterceptorContext context;
	private SpanContextInformation spanContext;

	@Before
	public void setUp() throws Exception {
		tracingPlugin = new TracingPlugin();
		final ConfigurationRegistry configuration = new ConfigurationRegistry(Collections.singletonList(tracingPlugin),
				Collections.singletonList(new SimpleSource()),
				null);

		spanContext = mock(SpanContextInformation.class);
		when(spanContext.isExternalRequest()).thenReturn(false);

		context = new PreExecutionInterceptorContext(spanContext);
		interceptor = new RateLimitingPreExecutionInterceptor();
		interceptor.init(configuration);
	}

	@Test
	public void testNeverReportServerSpan() throws Exception {
		tracingPlugin.getRateLimitServerSpansPerMinuteOption().update(0d, SimpleSource.NAME);
		when(spanContext.isExternalRequest()).thenReturn(false);
		when(spanContext.isServerRequest()).thenReturn(true);

		interceptor.interceptReport(context);
		assertFalse(context.isReport());
		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

	@Test
	public void testAlwaysReportServerSpan() throws Exception {
		when(spanContext.isExternalRequest()).thenReturn(false);
		when(spanContext.isServerRequest()).thenReturn(true);
		tracingPlugin.getRateLimitServerSpansPerMinuteOption().update(1_000_000.0, SimpleSource.NAME);

		interceptor.interceptReport(context);

		assertTrue(context.isReport());
	}

	@Test
	public void testRateNotExceededThenExceededServerSpan() throws Exception {
		when(spanContext.isExternalRequest()).thenReturn(false);
		when(spanContext.isServerRequest()).thenReturn(true);
		tracingPlugin.getRateLimitServerSpansPerMinuteOption().update(60d, SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertTrue(context.isReport());

		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

	@Test
	public void testNeverReportExternalRequest() throws Exception {
		when(spanContext.isExternalRequest()).thenReturn(true);
		when(spanContext.isServerRequest()).thenReturn(false);
		tracingPlugin.getRateLimitClientSpansPerMinuteOption().update(0d, SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

	@Test
	public void testAlwaysReportExternalRequest() throws Exception {
		when(spanContext.isExternalRequest()).thenReturn(true);
		when(spanContext.isServerRequest()).thenReturn(false);
		tracingPlugin.getRateLimitClientSpansPerMinuteOption().update(10_000_000.0, SimpleSource.NAME);

		interceptor.interceptReport(context);

		assertTrue(context.isReport());
	}

	@Test
	public void testRateExceededThenNotExceededExternalRequest() throws Exception {
		when(spanContext.isExternalRequest()).thenReturn(true);
		when(spanContext.isServerRequest()).thenReturn(false);
		tracingPlugin.getRateLimitClientSpansPerMinuteOption().update(60.0, SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertTrue(context.isReport());

		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

	@Test
	public void testReportExternalRequestGenericType() throws Exception {
		when(spanContext.isExternalRequest()).thenReturn(true);
		when(spanContext.isServerRequest()).thenReturn(false);
		when(spanContext.getOperationType()).thenReturn("jdbc");
		tracingPlugin.getRateLimitClientSpansPerMinuteOption().update(0d, SimpleSource.NAME);
		tracingPlugin.getRateLimitClientSpansPerTypePerMinuteOption().update(Collections.singletonMap("http", 1000000d), SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

	@Test
	public void testReportExternalRequestType() throws Exception {
		when(spanContext.isExternalRequest()).thenReturn(true);
		when(spanContext.isServerRequest()).thenReturn(false);
		when(spanContext.getOperationType()).thenReturn("http");
		tracingPlugin.getRateLimitClientSpansPerMinuteOption().update(0d, SimpleSource.NAME);
		tracingPlugin.getRateLimitClientSpansPerTypePerMinuteOption().update(Collections.singletonMap("http", 1000000d), SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertTrue(context.isReport());
	}

}
