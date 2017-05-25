package org.stagemonitor.tracing.sampling;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.source.SimpleSource;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;

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

		context = new PreExecutionInterceptorContext(spanContext);
		interceptor = new RateLimitingPreExecutionInterceptor();
		interceptor.init(configuration);
	}

	@Test
	public void testNeverReportSpan() throws Exception {
		tracingPlugin.getDefaultRateLimitSpansPerMinuteOption().update(0d, SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertFalse(context.isReport());
		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

	@Test
	public void testAlwaysReportSpan() throws Exception {
		tracingPlugin.getDefaultRateLimitSpansPerMinuteOption().update(1_000_000.0, SimpleSource.NAME);

		interceptor.interceptReport(context);

		assertTrue(context.isReport());
	}

	@Test
	public void testRateNotExceededThenExceededSpan() throws Exception {
		tracingPlugin.getDefaultRateLimitSpansPerMinuteOption().update(60d, SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertTrue(context.isReport());

		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

	@Test
	public void testRateExceededThenNotExceededSpan() throws Exception {
		tracingPlugin.getDefaultRateLimitSpansPerMinuteOption().update(60.0, SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertTrue(context.isReport());

		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

	@Test
	public void testReportSpanGenericType() throws Exception {
		when(spanContext.getOperationType()).thenReturn("jdbc");
		tracingPlugin.getDefaultRateLimitSpansPerMinuteOption().update(0d, SimpleSource.NAME);
		tracingPlugin.getRateLimitSpansPerMinutePerTypeOption().update(Collections.singletonMap("http", 1000000d), SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

	@Test
	public void testReportSpanType() throws Exception {
		when(spanContext.getOperationType()).thenReturn("http");
		tracingPlugin.getDefaultRateLimitSpansPerMinuteOption().update(0d, SimpleSource.NAME);
		tracingPlugin.getRateLimitSpansPerMinutePerTypeOption().update(Collections.singletonMap("http", 1000000d), SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertTrue(context.isReport());
	}

}
