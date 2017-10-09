package org.stagemonitor.web.servlet;

import com.uber.jaeger.context.TracingUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.tracing.wrapper.SpanWrappingTracer;
import org.stagemonitor.web.servlet.filter.StatusExposingByteCountingServletResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

import javax.servlet.FilterChain;

import io.opentracing.mock.MockTracer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MonitoredHttpRequestDoNotTrackTest {

	private ServletPlugin servletPlugin;
	private ConfigurationRegistry configuration;

	@Before
	public void setUp() throws Exception {
		configuration = mock(ConfigurationRegistry.class);
		CorePlugin corePlugin = mock(CorePlugin.class);
		TracingPlugin tracingPlugin = mock(TracingPlugin.class);
		this.servletPlugin = mock(ServletPlugin.class);

		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(TracingPlugin.class)).thenReturn(tracingPlugin);
		when(configuration.getConfig(ServletPlugin.class)).thenReturn(servletPlugin);
		when(tracingPlugin.getDefaultRateLimitSpansPerMinute()).thenReturn(1000000d);
		when(tracingPlugin.getOnlyReportSpansWithName()).thenReturn(Collections.emptyList());
		when(corePlugin.getElasticsearchUrl()).thenReturn("http://localhost:9200");
		when(corePlugin.getElasticsearchUrls()).thenReturn(Collections.singletonList("http://localhost:9200"));
		ElasticsearchClient elasticsearchClient = mock(ElasticsearchClient.class);
		when(elasticsearchClient.isElasticsearchAvailable()).thenReturn(true);
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient);
		when(corePlugin.getMetricRegistry()).thenReturn(new Metric2Registry());
		when(servletPlugin.isHonorDoNotTrackHeader()).thenReturn(true);
		when(tracingPlugin.getTracer()).thenReturn(new SpanWrappingTracer(new MockTracer(),
				Arrays.asList(
						new SpanContextInformation.SpanContextSpanEventListener(),
						new SpanContextInformation.SpanFinalizer()
				)
		));
		assertThat(TracingUtils.getTraceContext().isEmpty()).isTrue();
	}

	@After
	public void tearDown() throws Exception {
		assertThat(TracingUtils.getTraceContext().isEmpty()).isTrue();
	}

	@Test
	public void testHonorDoNotTrack() throws Exception {
		when(servletPlugin.isHonorDoNotTrackHeader()).thenReturn(true);
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("dnt", "1");
		new MonitoredHttpRequest(request, mock(StatusExposingByteCountingServletResponse.class),
				mock(FilterChain.class), configuration, mock(ExecutorService.class)).createSpan();
		SpanWrapper span = SpanContextInformation.getCurrent().getSpanWrapper();
		assertThat(span.isSampled()).isFalse();
		span.finish();
	}

	@Test
	public void testDoNotTrackDisabled() throws Exception {
		when(servletPlugin.isHonorDoNotTrackHeader()).thenReturn(true);
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("dnt", "0");
		new MonitoredHttpRequest(request, mock(StatusExposingByteCountingServletResponse.class),
				mock(FilterChain.class), configuration, mock(ExecutorService.class)).createSpan();
		SpanWrapper span = SpanContextInformation.getCurrent().getSpanWrapper();
		assertThat(span.isSampled()).isTrue();
		span.finish();
	}

	@Test
	public void testNoDoNotTrackHeader() throws Exception {
		when(servletPlugin.isHonorDoNotTrackHeader()).thenReturn(true);
		final MockHttpServletRequest request = new MockHttpServletRequest();
		new MonitoredHttpRequest(request, mock(StatusExposingByteCountingServletResponse.class),
				mock(FilterChain.class), configuration, mock(ExecutorService.class)).createSpan();
		SpanWrapper span = SpanContextInformation.getCurrent().getSpanWrapper();
		assertThat(span.isSampled()).isTrue();
		span.finish();
	}

	@Test
	public void testDontHonorDoNotTrack() throws Exception {
		when(servletPlugin.isHonorDoNotTrackHeader()).thenReturn(false);
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("dnt", "1");
		new MonitoredHttpRequest(request, mock(StatusExposingByteCountingServletResponse.class),
				mock(FilterChain.class), configuration, mock(ExecutorService.class)).createSpan();
		SpanWrapper span = SpanContextInformation.getCurrent().getSpanWrapper();
		assertThat(span.isSampled()).isTrue();
		span.finish();
	}

}
