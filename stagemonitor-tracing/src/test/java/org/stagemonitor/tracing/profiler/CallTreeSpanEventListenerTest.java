package org.stagemonitor.tracing.profiler;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.tracing.RequestMonitor;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.profiler.formatter.ShortSignatureFormatter;
import org.stagemonitor.tracing.sampling.PostExecutionInterceptorContext;
import org.stagemonitor.tracing.sampling.PreExecutionInterceptorContext;
import org.stagemonitor.tracing.sampling.SamplePriorityDeterminingSpanEventListener;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.tracing.wrapper.SpanWrappingTracer;

import java.util.Arrays;
import java.util.Collections;

import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CallTreeSpanEventListenerTest {

	private TracingPlugin tracingPlugin;
	private SpanWrapper span;
	private ConfigurationRegistry configurationRegistry;

	@Before
	public void setUp() throws Exception {
		tracingPlugin = mock(TracingPlugin.class);
		configurationRegistry = mock(ConfigurationRegistry.class);
		when(configurationRegistry.getConfig(TracingPlugin.class)).thenReturn(tracingPlugin);

		when(tracingPlugin.getProfilerRateLimitPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(tracingPlugin.isProfilerActive()).thenReturn(true);
		when(tracingPlugin.getCallTreeAsciiFormatter()).thenReturn(new ShortSignatureFormatter());
		final RequestMonitor requestMonitor = mock(RequestMonitor.class);
		doReturn(requestMonitor).when(tracingPlugin).getRequestMonitor();
	}

	@Test
	public void testProfileThisExecutionDeactive() throws Exception {
		doReturn(0d).when(tracingPlugin).getProfilerRateLimitPerMinute();
		final SpanContextInformation spanContext = invokeEventListener();
		assertThat(spanContext.getCallTree()).isNull();
	}

	@Test
	public void testProfileThisExecutionAlwaysActive() throws Exception {
		doReturn(1000000d).when(tracingPlugin).getProfilerRateLimitPerMinute();
		final SpanContextInformation spanContext = invokeEventListener();
		assertThat(spanContext.getCallTree()).isNotNull();
		assertThat(span.getStringTag(SpanUtils.CALL_TREE_JSON)).isNotNull();
		assertThat(span.getStringTag(SpanUtils.CALL_TREE_ASCII)).isNotNull();
	}

	@Test
	public void testExcludeCallTreeTags() throws Exception {
		doReturn(1000000d).when(tracingPlugin).getProfilerRateLimitPerMinute();
		when(tracingPlugin.getExcludedTags()).thenReturn(Arrays.asList(SpanUtils.CALL_TREE_JSON, SpanUtils.CALL_TREE_ASCII));
		final SpanContextInformation spanContext = invokeEventListener();
		assertThat(spanContext.getCallTree()).isNotNull();
		assertThat(span.getStringTag(SpanUtils.CALL_TREE_JSON)).isNull();
		assertThat(span.getStringTag(SpanUtils.CALL_TREE_ASCII)).isNull();
	}

	@Test
	public void testDontActivateProfilerWhenSpanIsNotSampled() throws Exception {
		doReturn(1000000d).when(tracingPlugin).getProfilerRateLimitPerMinute();
		final SpanContextInformation spanContext = invokeEventListener(false);
		assertThat(spanContext.getCallTree()).isNull();
	}

	private SpanContextInformation invokeEventListener() {
		return invokeEventListener(true);
	}

	private SpanContextInformation invokeEventListener(boolean sampled) {
		when(tracingPlugin.isSampled(any())).thenReturn(sampled);
		SpanWrappingTracer spanWrappingTracer = initTracer();
		final SpanWrappingTracer.SpanWrappingSpanBuilder spanBuilder = spanWrappingTracer.buildSpan("test");
		spanBuilder.withTag(Tags.SAMPLING_PRIORITY.getKey(), sampled ? 1 : 0);
		span = spanBuilder.startManual();
		final SpanContextInformation contextInformation = SpanContextInformation.forSpan(span);
		contextInformation.setPreExecutionInterceptorContext(new PreExecutionInterceptorContext(contextInformation));
		contextInformation.setPostExecutionInterceptorContext(new PostExecutionInterceptorContext(contextInformation));
		span.finish();
		return contextInformation;
	}

	private SpanWrappingTracer initTracer() {
		return new SpanWrappingTracer(new MockTracer(),
				Arrays.asList(
						new SpanContextInformation.SpanContextSpanEventListener(),
						new SamplePriorityDeterminingSpanEventListener(configurationRegistry, Collections.emptyList(), Collections.emptyList()),
						new CallTreeSpanEventListener(mock(Metric2Registry.class), tracingPlugin),
						new SpanContextInformation.SpanFinalizer())
		);
	}

	@Test
	public void testRateLimiting() throws Exception {
		when(tracingPlugin.getProfilerRateLimitPerMinute()).thenReturn(1d);
		final SpanContextInformation spanContext1 = invokeEventListener();
		assertNotNull(spanContext1.getCallTree());

		final SpanContextInformation spanContext2 = invokeEventListener();
		assertNotNull(spanContext2.getCallTree());

	}

}
