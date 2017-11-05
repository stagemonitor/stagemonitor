package org.stagemonitor.tracing.sampling;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.source.SimpleSource;
import org.stagemonitor.tracing.GlobalTracerTestHelper;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import java.util.Collections;

import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class RateLimitingPreExecutionInterceptorTest {

	private RateLimitingPreExecutionInterceptor interceptor;
	private TracingPlugin tracingPlugin;
	private PreExecutionInterceptorContext context;
	private SpanContextInformation spanContext;
	private ConfigurationRegistry configuration;

	@Before
	public void setUp() throws Exception {
		GlobalTracerTestHelper.resetGlobalTracer();
		tracingPlugin = spy(new TracingPlugin());
		doReturn(true).when(tracingPlugin).isRoot(any());
		configuration = new ConfigurationRegistry(Collections.singletonList(tracingPlugin),
				Collections.singletonList(new SimpleSource()),
				null);

		final Tracer tracer = mock(Tracer.class);
		GlobalTracer.register(tracer);

		spanContext = mock(SpanContextInformation.class);
		when(spanContext.getSpanWrapper()).thenReturn(mock(SpanWrapper.class));

		context = new PreExecutionInterceptorContext(spanContext);
		interceptor = new RateLimitingPreExecutionInterceptor();
		interceptor.init(configuration);
	}

	@After
	public void tearDown() throws Exception {
		GlobalTracerTestHelper.resetGlobalTracer();
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
	public void dontMakeSamplingDecisionsForNonRootTraces() throws Exception {
		interceptor = new RateLimitingPreExecutionInterceptor() {
			@Override
			protected boolean isRoot(SpanWrapper span) {
				return false;
			}
		};
		interceptor.init(configuration);
		tracingPlugin.getDefaultRateLimitSpansPerMinuteOption().update(0d, SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertTrue(context.isReport());
	}

	@Test
	public void makeSamplingDecisionsForRootTraces() throws Exception {
		interceptor = new RateLimitingPreExecutionInterceptor() {
			@Override
			protected boolean isRoot(SpanWrapper span) {
				return true;
			}
		};
		interceptor.init(configuration);
		tracingPlugin.getDefaultRateLimitSpansPerMinuteOption().update(0d, SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

}
