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

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ProbabilisticSamplingPreExecutionInterceptorTest {

	private ProbabilisticSamplingPreExecutionInterceptor interceptor;
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
		interceptor = new ProbabilisticSamplingPreExecutionInterceptor();
		interceptor.init(configuration);
	}

	@After
	public void tearDown() throws Exception {
		GlobalTracerTestHelper.resetGlobalTracer();
	}

	@Test
	public void testNeverReportSpan() throws Exception {
		tracingPlugin.getDefaultRateLimitSpansPercentOption().update(0d, SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertFalse(context.isReport());
		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

	@Test
	public void testAlwaysReportSpan() throws Exception {
		tracingPlugin.getDefaultRateLimitSpansPercentOption().update(1.0, SimpleSource.NAME);

		interceptor.interceptReport(context);

		assertTrue(context.isReport());
	}

	@Test
	public void testValidationFailed() throws Exception {
		assertThatThrownBy(() -> tracingPlugin.getDefaultRateLimitSpansPercentOption()
				.update(10.0, SimpleSource.NAME))
				.isInstanceOf(IllegalArgumentException.class);

		assertThat(tracingPlugin.getDefaultRateLimitSpansPercentOption().getValue()).isEqualTo(1.0);
	}

	@Test
	public void testValidationFailedPerTypeOption() throws Exception {
		assertThatThrownBy(() -> tracingPlugin.getRateLimitSpansPerMinutePercentPerTypeOption()
				.update(singletonMap("jdbc", 10.0), SimpleSource.NAME))
				.isInstanceOf(IllegalArgumentException.class);

		assertThat(tracingPlugin.getRateLimitSpansPerMinutePercentPerTypeOption().getValue()).isEqualTo(emptyMap());
	}

	@Test
	public void testSample51Percent() throws Exception {
		tracingPlugin.getDefaultRateLimitSpansPercentOption().update(0.51, SimpleSource.NAME);

		int reports = 0;
		for (int i = 0; i < 100; i++) {
			final PreExecutionInterceptorContext context = new PreExecutionInterceptorContext(spanContext);
			interceptor.interceptReport(context);
			if (context.isReport()) {
				reports++;
			}
		}

		assertThat(reports).isEqualTo(51);
	}

	@Test
	public void testReportSpanGenericType() throws Exception {
		when(spanContext.getOperationType()).thenReturn("jdbc");
		tracingPlugin.getDefaultRateLimitSpansPercentOption().update(0d, SimpleSource.NAME);
		tracingPlugin.getRateLimitSpansPerMinutePercentPerTypeOption().update(singletonMap("http", 1d), SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

	@Test
	public void testReportSpanType() throws Exception {
		interceptor = new ProbabilisticSamplingPreExecutionInterceptor() {
			@Override
			protected boolean isRoot(SpanWrapper span) {
				return false;
			}
		};
		interceptor.init(configuration);

		when(spanContext.getOperationType()).thenReturn("http");
		tracingPlugin.getDefaultRateLimitSpansPercentOption().update(0d, SimpleSource.NAME);
		tracingPlugin.getRateLimitSpansPerMinutePercentPerTypeOption().update(singletonMap("http", 1d), SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertTrue(context.isReport());
	}

	@Test
	public void dontMakeSamplingDecisionsForNonRootTraces() throws Exception {
		interceptor = new ProbabilisticSamplingPreExecutionInterceptor() {
			@Override
			protected boolean isRoot(SpanWrapper span) {
				return false;
			}
		};
		interceptor.init(configuration);
		tracingPlugin.getDefaultRateLimitSpansPercentOption().update(0d, SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertTrue(context.isReport());
	}

	@Test
	public void makeSamplingDecisionsForRootTraces() throws Exception {
		interceptor = new ProbabilisticSamplingPreExecutionInterceptor() {
			@Override
			protected boolean isRoot(SpanWrapper span) {
				return true;
			}
		};
		interceptor.init(configuration);
		tracingPlugin.getDefaultRateLimitSpansPercentOption().update(0d, SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}
}
