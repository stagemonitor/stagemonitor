package org.stagemonitor.tracing.sampling;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.source.SimpleSource;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;

import java.util.Collections;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProbabilisticSamplingPreExecutionInterceptorTest {

	private ProbabilisticSamplingPreExecutionInterceptor interceptor;
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
		interceptor = new ProbabilisticSamplingPreExecutionInterceptor();
		interceptor.init(configuration);
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
		assertThatThrownBy(() -> tracingPlugin.getRateLimitSpansPerMinutePercentOption()
				.update(singletonMap("jdbc", 10.0), SimpleSource.NAME))
				.isInstanceOf(IllegalArgumentException.class);

		assertThat(tracingPlugin.getRateLimitSpansPerMinutePercentOption().getValue()).isEqualTo(emptyMap());
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
		tracingPlugin.getRateLimitSpansPerMinutePercentOption().update(singletonMap("http", 1d), SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertFalse(context.isReport());
	}

	@Test
	public void testReportSpanType() throws Exception {
		when(spanContext.getOperationType()).thenReturn("http");
		tracingPlugin.getDefaultRateLimitSpansPercentOption().update(0d, SimpleSource.NAME);
		tracingPlugin.getRateLimitSpansPerMinutePercentOption().update(singletonMap("http", 1d), SimpleSource.NAME);

		interceptor.interceptReport(context);
		assertTrue(context.isReport());
	}

}
